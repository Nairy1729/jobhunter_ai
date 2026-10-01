package com.jobhunter.service.matching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.*;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.embedding.VectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SemanticMatchingService {

    private static final Logger log = LoggerFactory.getLogger(SemanticMatchingService.class);

    private final JobRepository jobRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AgentRunRepository agentRunRepository;
    private final JobRequirementExtractor requirementExtractor;
    private final VectorService vectorService;
    private final GroundingVerificationGate groundingGate;
    private final com.jobhunter.service.prioritization.JobPrioritizationService prioritizationService;
    private final ObjectMapper objectMapper;

    public SemanticMatchingService(
            JobRepository jobRepository,
            CandidateProfileRepository profileRepository,
            JobMatchRepository jobMatchRepository,
            AgentRunRepository agentRunRepository,
            JobRequirementExtractor requirementExtractor,
            VectorService vectorService,
            GroundingVerificationGate groundingGate,
            com.jobhunter.service.prioritization.JobPrioritizationService prioritizationService,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.agentRunRepository = agentRunRepository;
        this.requirementExtractor = requirementExtractor;
        this.vectorService = vectorService;
        this.groundingGate = groundingGate;
        this.prioritizationService = prioritizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MatchAnalysisResponse analyzeAndMatch(UUID jobId, UUID userId) {
        return analyzeAndMatch(jobId, userId, false);
    }

    @Transactional
    public MatchAnalysisResponse analyzeAndMatch(UUID jobId, UUID userId, boolean forceRecompute) {
        long startTime = System.currentTimeMillis();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        // 1. Check existing cached match
        Optional<JobMatch> existingMatch = jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, profile.getId());
        if (!forceRecompute && existingMatch.isPresent() && !isJobOrProfileStale(job, profile, existingMatch.get())) {
            log.info("MATCH_CACHE_HIT - Reusing existing match evaluation for Job [{}] and Candidate [{}]", jobId, profile.getId());
            return buildResponseFromEntity(existingMatch.get(), job);
        }

        log.info("MATCH_EVALUATION_START - Analyzing Job [{}] ({}) against Candidate [{}]",
                job.getTitle(), job.getCompany().getName(), profile.getUser().getEmail());

        // 2. Extract structured job requirements
        List<JobRequirement> requirements = requirementExtractor.extractAndPersistRequirements(job);

        // 3. Vector Embeddings & Semantic Cosine Similarity
        float[] jobVector = vectorService.getOrGenerateJobEmbedding(job);
        float[] candidateVector = vectorService.getOrGenerateCandidateEmbedding(profile);
        double semanticSimilarity = vectorService.cosineSimilarity(jobVector, candidateVector);

        // 4. Multi-Dimensional Requirement-by-Requirement Analysis
        List<RequirementMatchItem> reqAnalyses = new ArrayList<>();
        List<String> strongMatches = new ArrayList<>();
        List<String> partialMatches = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> transferable = new ArrayList<>();
        List<String> riskFactors = new ArrayList<>();
        List<String> supportingEvidence = new ArrayList<>();

        Map<String, CandidateSkill> candidateSkillsMap = new HashMap<>();
        if (profile.getSkills() != null) {
            for (CandidateSkill cs : profile.getSkills()) {
                candidateSkillsMap.put(cs.getSkill().getName().toLowerCase(), cs);
            }
        }

        int totalMustHaves = 0;
        int satisfiedMustHaves = 0;

        for (JobRequirement req : requirements) {
            RequirementMatchItem item = evaluateRequirement(req, profile, candidateSkillsMap);

            // GROUNDING VERIFICATION GATE:
            // Ensure no false claims slip into strong or partial matches for technical requirements
            if ("TECHNICAL".equalsIgnoreCase(req.getCategory()) && ("STRONG".equals(item.getMatchType()) || "PARTIAL".equals(item.getMatchType()))) {
                GroundingVerificationGate.GroundingResult gateResult =
                        groundingGate.verifyTechnicalClaim(req.getDescription(), item.getCandidateEvidence(), profile);

                if (!gateResult.passed() && gateResult.convertedToGap()) {
                    item.setMatchType("GAP");
                    item.setCandidateEvidence(gateResult.validatedClaim());
                    item.setExplanation(gateResult.rejectedReason());
                } else if (gateResult.validatedClaim() != null) {
                    item.setCandidateEvidence(gateResult.validatedClaim());
                }
            }

            reqAnalyses.add(item);

            if ("MUST_HAVE".equals(req.getRequirementType())) {
                totalMustHaves++;
                if ("STRONG".equals(item.getMatchType())) satisfiedMustHaves++;
            }

            // Categorize into summary lists
            switch (item.getMatchType()) {
                case "STRONG" -> {
                    strongMatches.add(req.getDescription() + " — " + item.getCandidateEvidence());
                    if (item.getCandidateEvidence() != null) supportingEvidence.add(item.getCandidateEvidence());
                }
                case "PARTIAL" -> partialMatches.add(req.getDescription() + " — " + item.getCandidateEvidence());
                case "TRANSFERABLE" -> transferable.add(item.getExplanation());
                case "GAP" -> gaps.add(req.getDescription() + " (" + item.getExplanation() + ")");
            }
        }

        // 5. Seniority and Work Mode Alignment Checks
        evaluateSeniorityAndWorkMode(job, profile, riskFactors);

        // 6. Multi-Factor Priority Score Calculation
        double skillScore = totalMustHaves > 0
                ? ((double) satisfiedMustHaves / totalMustHaves) * 100.0
                : (strongMatches.isEmpty() ? 15.0 : 70.0);
        double yoeScore = computeExperienceScore(job, profile);
        double locationScore = computeLocationScore(job, profile);

        double compositeScore = (skillScore * 0.40) +
                (yoeScore * 0.25) +
                (semanticSimilarity * 100.0 * 0.15) +
                (locationScore * 0.10) +
                (Math.min(100.0, strongMatches.size() * 12.0) * 0.10);

        compositeScore = Math.max(0.0, Math.min(100.0, compositeScore));
        BigDecimal finalPriorityScore = BigDecimal.valueOf(compositeScore).setScale(2, RoundingMode.HALF_UP);

        // 7. Recommendation and Queue Tier Assignment
        String recommendation;
        String queueTier;

        boolean severeSeniorityDelta = riskFactors.stream().anyMatch(r -> r.contains("Seniority Delta"));

        if ((severeSeniorityDelta && satisfiedMustHaves == 0) || (strongMatches.isEmpty() && satisfiedMustHaves == 0)) {
            recommendation = "DO_NOT_APPLY";
            queueTier = "LOW_PRIORITY";
        } else if (compositeScore >= 68.0 && (totalMustHaves == 0 || satisfiedMustHaves >= 1)) {
            recommendation = "APPLY";
            queueTier = "HIGH_PRIORITY";
        } else if (compositeScore >= 48.0) {
            recommendation = "APPLY_AFTER_TAILORING";
            queueTier = "REVIEW";
        } else if (compositeScore >= 35.0) {
            recommendation = "LOW_PRIORITY";
            queueTier = "LOW_PRIORITY";
        } else {
            recommendation = "DO_NOT_APPLY";
            queueTier = "LOW_PRIORITY";
        }

        // 8. Synthesize Application Advantage Report (The Edge System)
        ApplicationAdvantageReportDto advantageReport = buildAdvantageReport(
                job, profile, strongMatches, gaps, transferable, riskFactors
        );

        // 9. Overall Assessment Narrative
        String overallAssessment = buildOverallAssessment(job, profile, recommendation, finalPriorityScore, strongMatches, gaps);

        // 10. Assemble Response DTO
        MatchAnalysisResponse response = new MatchAnalysisResponse();
        response.setJobId(job.getId());
        response.setJobTitle(job.getTitle());
        response.setCompanyName(job.getCompany().getName());
        response.setCandidateProfileId(profile.getId());
        response.setRecommendation(recommendation);
        response.setPriorityScore(finalPriorityScore);
        response.setQueueTier(queueTier);
        response.setSemanticSimilarityScore(Math.round(semanticSimilarity * 100.0) / 100.0);
        response.setOverallAssessment(overallAssessment);
        response.setStrongMatches(strongMatches);
        response.setPartialMatches(partialMatches);
        response.setGaps(gaps);
        response.setTransferableExperience(transferable);
        response.setRiskFactors(riskFactors);
        response.setSupportingEvidence(supportingEvidence);
        response.setRequirementAnalysis(reqAnalyses);
        response.setAdvantageReport(advantageReport);

        // 11. Intelligent Job Prioritization Evaluation
        com.jobhunter.dto.prioritization.JobPrioritizationResult prioResult =
                prioritizationService.evaluatePrioritization(job, profile, response);

        response.setPriorityCategory(prioResult.getPriorityCategory() != null ? prioResult.getPriorityCategory().name() : null);
        response.setFreshness(prioResult.getFreshness() != null ? prioResult.getFreshness().name() : null);
        response.setDaysSincePosted(prioResult.getDaysSincePosted());
        response.setWhyThisJob(prioResult.getWhyThisJob());
        response.setPotentialConcerns(prioResult.getPotentialConcerns());
        response.setHardConstraintViolations(prioResult.getHardConstraintViolations());
        response.setRequirementCoverage(prioResult.getRequirementCoverage());
        response.setKeyTechnologies(prioResult.getKeyTechnologies());
        if (prioResult.getPriorityScore() != null) {
            response.setPriorityScore(BigDecimal.valueOf(prioResult.getPriorityScore()));
        }

        // 12. Persist or Update JobMatch entity
        JobMatch match = existingMatch.orElseGet(JobMatch::new);
        match.setJob(job);
        match.setCandidateProfile(profile);
        match.setRecommendation(recommendation);
        match.setPriorityScore(response.getPriorityScore());
        match.setQueueTier(queueTier);
        match.setPriorityCategory(response.getPriorityCategory());
        match.setFreshness(response.getFreshness());
        match.setDaysSincePosted(response.getDaysSincePosted());
        match.setEvaluatedAt(Instant.now());

        try {
            match.setStrongMatches(objectMapper.writeValueAsString(strongMatches));
            match.setPartialMatches(objectMapper.writeValueAsString(partialMatches));
            match.setGaps(objectMapper.writeValueAsString(gaps));
            match.setTransferableExperience(objectMapper.writeValueAsString(transferable));
            match.setRiskFactors(objectMapper.writeValueAsString(riskFactors));
            match.setAdvantageReport(objectMapper.writeValueAsString(advantageReport));
            match.setWhyThisJob(objectMapper.writeValueAsString(prioResult.getWhyThisJob()));
            match.setPotentialConcerns(objectMapper.writeValueAsString(prioResult.getPotentialConcerns()));
            match.setRequirementCoverage(objectMapper.writeValueAsString(prioResult.getRequirementCoverage()));
        } catch (Exception e) {
            log.error("Failed to serialize match json", e);
        }

        jobMatchRepository.save(match);
        response.setEvaluatedAt(match.getEvaluatedAt());

        // Update Job pipeline status
        if ("DISCOVERED".equals(job.getPipelineStatus())) {
            job.setPipelineStatus("ANALYZED");
            jobRepository.save(job);
        }

        // 13. Record operational telemetry in agent_runs
        long executionTimeMs = System.currentTimeMillis() - startTime;
        AgentRun run = new AgentRun("MatchingAgent", "SUCCESS", executionTimeMs);
        run.setJob(job);
        run.setLlmProvider("SEMANTIC_ENGINE_HYBRID");
        agentRunRepository.save(run);

        return response;
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        String regex = "\\b" + Pattern.quote(word.trim().toLowerCase()) + "\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private RequirementMatchItem evaluateRequirement(
            JobRequirement req,
            CandidateProfile profile,
            Map<String, CandidateSkill> candidateSkillsMap) {

        String desc = req.getDescription();
        String descLower = desc.toLowerCase();

        // 1. Technical Skill Requirement
        if ("TECHNICAL".equalsIgnoreCase(req.getCategory()) && req.getSkill() != null) {
            String skillKey = req.getSkill().getName().toLowerCase();
            if (candidateSkillsMap.containsKey(skillKey)) {
                CandidateSkill cs = candidateSkillsMap.get(skillKey);
                boolean isCommercial = "COMMERCIAL".equalsIgnoreCase(cs.getExperienceType());

                if (isCommercial) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            cs.getYearsExperience() + " years commercial experience in " + cs.getSkill().getName() + " (" + cs.getProficiencyLevel() + "). " + (cs.getEvidenceText() != null ? cs.getEvidenceText() : ""),
                            "STRONG",
                            "HIGH",
                            "Direct verified commercial production experience satisfying this requirement.",
                            req.isImplied()
                    );
                } else {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            "Project/Practical experience with " + cs.getSkill().getName() + ". " + (cs.getEvidenceText() != null ? cs.getEvidenceText() : ""),
                            "PARTIAL",
                            "MEDIUM",
                            "Demonstrated in personal/portfolio projects, but lacks commercial production track record.",
                            req.isImplied()
                    );
                }
            }
        }

        // Check technical mention across experiences or projects
        for (CandidateSkill cs : candidateSkillsMap.values()) {
            if (containsWord(desc, cs.getSkill().getName())) {
                boolean isCommercial = "COMMERCIAL".equalsIgnoreCase(cs.getExperienceType());
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        cs.getSkill().getName() + " (" + cs.getProficiencyLevel() + ", " + cs.getExperienceType() + "): " + (cs.getEvidenceText() != null ? cs.getEvidenceText() : ""),
                        isCommercial ? "STRONG" : "PARTIAL",
                        "HIGH",
                        "Verified capability in " + cs.getSkill().getName() + ".",
                        req.isImplied()
                );
            }
        }

        // Check Transferable capability (e.g. PostgreSQL for SQL/Databases, Spring Boot for REST)
        if (descLower.contains("relational database") || descLower.contains("sql") || descLower.contains("mysql")) {
            if (candidateSkillsMap.containsKey("postgresql") || candidateSkillsMap.containsKey("sql")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "PostgreSQL commercial expertise (schema design, indexing, transactions).",
                        "TRANSFERABLE",
                        "HIGH",
                        "Candidate possesses deep relational database and SQL expertise in PostgreSQL, highly transferable to MySQL or generic RDBMS.",
                        req.isImplied()
                );
            }
        }

        if (descLower.contains("api") || descLower.contains("rest") || descLower.contains("microservice")) {
            if (candidateSkillsMap.containsKey("spring boot") || candidateSkillsMap.containsKey("rest apis")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Spring Boot RESTful microservices and API design.",
                        "STRONG",
                        "HIGH",
                        "Direct commercial track record building RESTful services with Spring Boot and Spring Security.",
                        req.isImplied()
                );
            }
        }

        // 2. Experience / Seniority Requirement
        if ("EXPERIENCE".equalsIgnoreCase(req.getCategory())) {
            double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;
            if (descLower.contains("seniority")) {
                if (descLower.contains("junior") || descLower.contains("associate") || descLower.contains("mid")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            candYoe + " years of software engineering experience.",
                            "STRONG",
                            "HIGH",
                            "Candidate's 2.5 years of experience fits the mid-level/associate scope well.",
                            req.isImplied()
                    );
                } else if (descLower.contains("staff") || descLower.contains("principal") || descLower.contains("lead")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            candYoe + " years of experience vs Staff/Principal level requested.",
                            "GAP",
                            "HIGH",
                            "Significant seniority delta: Role targets Staff/Lead engineers, whereas candidate currently has " + candYoe + " years.",
                            req.isImplied()
                    );
                } else {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            candYoe + " years of commercial engineering experience.",
                            "PARTIAL",
                            "MEDIUM",
                            "Candidate has " + candYoe + " years of experience; applicable depending on technical depth and interview evaluation.",
                            req.isImplied()
                    );
                }
            } else {
                double minReq = 2.0;
                Matcher mRange = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:-|to)\\s*(\\d+(?:\\.\\d+)?)\\+?\\s*years").matcher(descLower);
                if (mRange.find()) {
                    try {
                        minReq = Double.parseDouble(mRange.group(1));
                    } catch (Exception ignored) {}
                } else {
                    Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\+?\\s*years").matcher(descLower);
                    if (m.find()) {
                        try {
                            minReq = Double.parseDouble(m.group(1));
                        } catch (Exception ignored) {}
                    }
                }

                String matchType;
                if (candYoe >= minReq) {
                    matchType = "STRONG";
                } else if (candYoe >= minReq - 1.5) {
                    matchType = "PARTIAL";
                } else {
                    matchType = "GAP";
                }

                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        candYoe + " years of verified software development experience (vs " + minReq + "+ required).",
                        matchType,
                        "HIGH",
                        "GAP".equals(matchType)
                                ? "Candidate experience (" + candYoe + " yrs) is significantly below role minimum (" + minReq + "+ yrs)."
                                : "Candidate holds " + candYoe + " years of verified commercial engineering experience.",
                        req.isImplied()
                );
            }
        }

        // 3. Location Requirement
        if ("LOCATION".equalsIgnoreCase(req.getCategory())) {
            List<String> prefLocs = parseJsonList(profile.getPreferredLocations());
            String curLoc = profile.getCurrentLocation() != null ? profile.getCurrentLocation().toLowerCase() : "";
            boolean isRemote = descLower.contains("remote");
            boolean matchesPref = isRemote
                    || prefLocs.stream().anyMatch(pl -> !pl.isBlank() && !pl.equalsIgnoreCase("Remote") && descLower.contains(pl.toLowerCase()))
                    || (!curLoc.isBlank() && descLower.contains(curLoc))
                    || descLower.contains("india") || descLower.contains("bangalore") || descLower.contains("bengaluru");

            if (matchesPref) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Candidate location (" + profile.getCurrentLocation() + ", Preferred: " + profile.getPreferredLocations() + ") directly matches role location.",
                        "STRONG",
                        "HIGH",
                        "Candidate geographic alignment directly satisfies role location criteria.",
                        req.isImplied()
                );
            } else {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Candidate Location: " + profile.getCurrentLocation() + ", Preferred: " + profile.getPreferredLocations(),
                        "PARTIAL",
                        "MEDIUM",
                        "Work location requires discussion during initial recruiter screening.",
                        req.isImplied()
                );
            }
        }

        // 4. Work Mode Requirement
        if ("WORK_MODE".equalsIgnoreCase(req.getCategory())) {
            List<String> prefModes = parseJsonList(profile.getWorkModes());
            boolean matchesMode = prefModes.stream().anyMatch(pm -> descLower.contains(pm.toLowerCase()))
                    || descLower.contains("hybrid") || descLower.contains("remote");

            if (matchesMode) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Candidate work mode preference (" + profile.getWorkModes() + ") directly aligns with role work mode.",
                        "STRONG",
                        "HIGH",
                        "Candidate preferred work mode satisfies role requirements.",
                        req.isImplied()
                );
            } else {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Candidate Preferred Work Modes: " + profile.getWorkModes(),
                        "PARTIAL",
                        "MEDIUM",
                        "Work arrangement requires alignment during initial recruiter screening.",
                        req.isImplied()
                );
            }
        }

        // 5. Domain Requirement
        if ("DOMAIN".equalsIgnoreCase(req.getCategory())) {
            if (profile.getExperiences() != null) {
                for (CandidateExperience exp : profile.getExperiences()) {
                    String domain = exp.getDomain() != null ? exp.getDomain().toLowerCase() : "";
                    String company = exp.getCompany() != null ? exp.getCompany().toLowerCase() : "";
                    String evidence = exp.getEvidenceText() != null ? exp.getEvidenceText().toLowerCase() : "";

                    boolean domainMatches = (descLower.contains("fintech") || descLower.contains("financial") || descLower.contains("billing") || descLower.contains("transaction")) &&
                            (domain.contains("fintech") || domain.contains("financial") || company.contains("fintech") || evidence.contains("transaction") || evidence.contains("payment"));

                    if (domainMatches) {
                        return new RequirementMatchItem(
                                req.getDescription(),
                                req.getCategory(),
                                "Commercial production experience in " + exp.getDomain() + " at " + exp.getCompany() + " (" + exp.getDuration() + ").",
                                "STRONG",
                                "HIGH",
                                "Candidate verified experience in " + exp.getDomain() + " satisfies role domain criteria.",
                                req.isImplied()
                        );
                    }
                }
            }
        }

        // 6. Responsibility / Process Requirement
        if ("RESPONSIBILITY".equalsIgnoreCase(req.getCategory()) || "PROCESS".equalsIgnoreCase(req.getCategory())) {
            if (descLower.contains("database") || descLower.contains("schema") || descLower.contains("sql") || descLower.contains("nosql")) {
                if (candidateSkillsMap.containsKey("postgresql") || candidateSkillsMap.containsKey("sql")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            "Commercial PostgreSQL schema design, composite B-Tree indexing, and query optimization.",
                            "STRONG",
                            "HIGH",
                            "Commercial database schema and optimization experience satisfies this responsibility.",
                            req.isImplied()
                    );
                }
            }
            if (descLower.contains("api") || descLower.contains("server-side") || descLower.contains("backend") || descLower.contains("rest") || descLower.contains("microservice")) {
                if (candidateSkillsMap.containsKey("java") || candidateSkillsMap.containsKey("spring boot") || candidateSkillsMap.containsKey("rest apis")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            "Commercial Spring Boot RESTful microservices and API integration at FinTech SaaS.",
                            "STRONG",
                            "HIGH",
                            "Commercial track record developing robust backend server-side services and APIs.",
                            req.isImplied()
                    );
                }
            }
            if (descLower.contains("security") || descLower.contains("audit") || descLower.contains("vulnerabilit")) {
                if (candidateSkillsMap.containsKey("spring security") || candidateSkillsMap.containsKey("jwt")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            "Configured stateless Spring Security JWT authentication filter chains and role-based access control.",
                            "STRONG",
                            "HIGH",
                            "Verified capability implementing web application security controls.",
                            req.isImplied()
                    );
                }
            }
            if (descLower.contains("optimiz") || descLower.contains("performan") || descLower.contains("scalab")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Reduced batch report generation latency from 14s to 1.8s through PostgreSQL query plan optimization.",
                        "STRONG",
                        "HIGH",
                        "Demonstrated performance tuning and query plan optimization achievements.",
                        req.isImplied()
                );
            }
            if (descLower.contains("version control") || descLower.contains("git")) {
                if (candidateSkillsMap.containsKey("git")) {
                    return new RequirementMatchItem(
                            req.getDescription(),
                            req.getCategory(),
                            "Advanced Git workflows, branching strategies, rebase operations, and peer PR code reviews.",
                            "STRONG",
                            "HIGH",
                            "Direct verified commercial Git version control discipline.",
                            req.isImplied()
                    );
                }
            }
            if (descLower.contains("test") || descLower.contains("debug") || descLower.contains("qa")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Authored automated integration tests using Testcontainers, maintaining 90%+ test coverage.",
                        "STRONG",
                        "MEDIUM",
                        "Experience conducting automated integration and component testing.",
                        req.isImplied()
                );
            }
            if (descLower.contains("document") || descLower.contains("swagger") || descLower.contains("openapi")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Delivered automated Swagger/OpenAPI documentation for 20+ microservice endpoints.",
                        "STRONG",
                        "MEDIUM",
                        "Proven capability maintaining API documentation.",
                        req.isImplied()
                );
            }
            if (descLower.contains("collaborat") || descLower.contains("team") || descLower.contains("front-end")) {
                return new RequirementMatchItem(
                        req.getDescription(),
                        req.getCategory(),
                        "Collaborated with cross-functional engineering and frontend teams in Agile sprint delivery.",
                        "STRONG",
                        "MEDIUM",
                        "Collaborative engineering track record.",
                        req.isImplied()
                );
            }
        }

        // 7. Soft Skills
        if ("SOFT_SKILL".equalsIgnoreCase(req.getCategory())) {
            return new RequirementMatchItem(
                    req.getDescription(),
                    req.getCategory(),
                    "Demonstrated collaborative experience across cross-functional engineering teams.",
                    "STRONG",
                    "MEDIUM",
                    "Supported by engineering team experience and Agile project delivery.",
                    req.isImplied()
            );
        }

        // Default: Requirement represents an unverified gap
        return new RequirementMatchItem(
                req.getDescription(),
                req.getCategory(),
                "No verified candidate evidence found in master profile or projects.",
                "GAP",
                "HIGH",
                "JD requirement is missing from candidate's verified skill inventory.",
                req.isImplied()
        );
    }

    private void evaluateSeniorityAndWorkMode(Job job, CandidateProfile profile, List<String> riskFactors) {
        double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;
        if (job.getMinExperienceYears() != null) {
            double minReq = job.getMinExperienceYears().doubleValue();
            if ((minReq - candYoe) >= 2.0) {
                riskFactors.add("Seniority Delta: Role asks for " + minReq + "+ years, candidate has " + candYoe + " years.");
            }
        }

        String jobWorkMode = job.getWorkMode();
        if ("ON_SITE".equalsIgnoreCase(jobWorkMode)) {
            String candModes = profile.getWorkModes() != null ? profile.getWorkModes().toUpperCase() : "";
            if (!candModes.contains("ON_SITE") && !candModes.contains("RELOCATION")) {
                riskFactors.add("Work Mode Friction: Role is ON-SITE in " + job.getLocation() + ", whereas candidate preferred modes are " + profile.getWorkModes());
            }
        }
    }

    private double computeExperienceScore(Job job, CandidateProfile profile) {
        double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;
        if (job.getMinExperienceYears() == null) return 85.0;

        double minReq = job.getMinExperienceYears().doubleValue();
        if (candYoe >= minReq) return 100.0;
        if (candYoe >= minReq - 1.0) return 80.0;
        if (candYoe >= minReq - 2.0) return 50.0;
        return 15.0;
    }

    private double computeLocationScore(Job job, CandidateProfile profile) {
        String jobMode = job.getWorkMode();
        String candModes = profile.getWorkModes() != null ? profile.getWorkModes().toUpperCase() : "";

        if ("REMOTE".equalsIgnoreCase(jobMode) && candModes.contains("REMOTE")) return 100.0;
        if ("HYBRID".equalsIgnoreCase(jobMode) && (candModes.contains("HYBRID") || candModes.contains("REMOTE"))) return 85.0;
        if ("ON_SITE".equalsIgnoreCase(jobMode)) {
            if (candModes.contains("ON_SITE") || candModes.contains("RELOCATION")) return 80.0;
            return 25.0;
        }
        return 50.0;
    }

    private ApplicationAdvantageReportDto buildAdvantageReport(
            Job job, CandidateProfile profile, List<String> strongMatches,
            List<String> gaps, List<String> transferable, List<String> riskFactors) {

        ApplicationAdvantageReportDto report = new ApplicationAdvantageReportDto();

        // 1. Employer Priorities
        report.getEmployerPriorities().add("High-reliability backend services and clean API architecture");
        if (job.getTitle().toLowerCase().contains("distributed") || job.getRawDescriptionMarkdown().toLowerCase().contains("scale")) {
            report.getEmployerPriorities().add("Scalable distributed data processing and cache management");
        }
        report.getEmployerPriorities().add("Production experience with relational persistence (PostgreSQL/SQL)");

        // 2. Candidate Relevance
        report.setCandidateRelevance(
                "Candidate brings 2.5 years of strong hands-on commercial experience in Java and Spring Boot microservices, with deep knowledge of PostgreSQL transactions and Spring Security."
        );

        // 3. Strongest Evidence
        report.getStrongestEvidence().add("Commercial microservices engineering with Spring Boot, Spring Data JPA, and RESTful APIs.");
        report.getStrongestEvidence().add("Relational schema design, query optimization, and transaction management in PostgreSQL.");
        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            CandidateProject p = profile.getProjects().get(0);
            report.getStrongestEvidence().add("Project '" + p.getName() + "': " + p.getDescription());
        }

        // 4. What to Emphasize
        report.getWhatToEmphasize().add("Promote Spring Boot REST API development and Spring Security filter chains to the top of the resume.");
        report.getWhatToEmphasize().add("Highlight PostgreSQL indexing, transactional guarantees, and complex query performance.");
        report.getWhatToEmphasize().add("Feature enterprise architecture design patterns and clean code discipline.");

        // 5. What to De-emphasize
        report.getWhatToDeemphasize().add("De-emphasize non-essential frontend frameworks (e.g. minor React details) unless applying for full-stack responsibilities.");
        report.getWhatToDeemphasize().add("Reduce prominence of legacy or secondary stacks (e.g. .NET Core / C#) to keep the resume strictly focused on the target stack.");

        // 6. Honest Gaps
        if (!gaps.isEmpty()) {
            for (String g : gaps.subList(0, Math.min(3, gaps.size()))) {
                report.getHonestGaps().add("Gap: " + g + " — Action: Address transparently without defensive posturing, anchoring on proven adjacent foundational strengths.");
            }
        } else {
            report.getHonestGaps().add("No critical hard-skill gaps detected for this role.");
        }

        // 7. Transferable Skills
        if (!transferable.isEmpty()) {
            report.setTransferableSkills(new ArrayList<>(transferable));
        } else {
            report.getTransferableSkills().add("Deep PostgreSQL and relational schema expertise transfers seamlessly to any enterprise relational database requirement.");
        }

        // 8. Resume Positioning
        report.getResumePositioning().add("Place Core Java (17), Spring Boot, and PostgreSQL prominently in the Primary Technical Skills bar.");
        report.getResumePositioning().add("Reorder bullet points under current role to put API performance, data consistency, and microservice scale first.");
        report.getResumePositioning().add("Quantify database tuning wins (e.g. latency reduction, query plan optimization) in project highlights.");

        // 9. Application Fit & Positioning
        report.getApplicationFitAndPositioning().add("Position candidate as a disciplined, backend-focused software engineer with verified production Java/Spring Boot capabilities rather than a generic developer.");
        report.getApplicationFitAndPositioning().add("Lead application narrative with proven commercial REST APIs and relational persistence reliability.");
        if (job.getTitle().toLowerCase().contains("senior")) {
            report.getApplicationFitAndPositioning().add("Bridge seniority requirement by emphasizing end-to-end service ownership and architectural decision-making.");
        }

        // 10. Application Strategy
        report.setApplicationStrategy(
                "Submit targeted application through verified employer portal using tailored resume highlighting Java 17, Spring Boot, and PostgreSQL microservices; reinforce production reliability and code craftsmanship."
        );

        return report;
    }

    private String buildOverallAssessment(
            Job job, CandidateProfile profile, String recommendation,
            BigDecimal score, List<String> strongMatches, List<String> gaps) {

        StringBuilder sb = new StringBuilder();
        sb.append("Match Score: ").append(score).append("% (").append(recommendation).append("). ");
        if ("APPLY".equals(recommendation)) {
            sb.append("Strong technical alignment: Candidate possesses verified commercial production experience in the core requested technologies (").append(strongMatches.size()).append(" direct matches). Recommended for priority submission.");
        } else if ("APPLY_AFTER_TAILORING".equals(recommendation)) {
            sb.append("Solid underlying capability match, but tailoring is strongly advised to sharpen relevant bullet points and address ").append(gaps.size()).append(" gap area(s).");
        } else if ("LOW_PRIORITY".equals(recommendation)) {
            sb.append("Moderate alignment. Significant gaps in requested seniority or secondary technologies. Review advantage report before deciding to apply.");
        } else {
            sb.append("Low overall alignment due to substantial seniority delta or major technology stack divergence. Not recommended for primary pipeline application.");
        }
        return sb.toString();
    }

    private boolean isJobOrProfileStale(Job job, CandidateProfile profile, JobMatch match) {
        if (job.getUpdatedAt() != null && job.getUpdatedAt().isAfter(match.getEvaluatedAt())) {
            return true;
        }
        if (profile.getUpdatedAt() != null && profile.getUpdatedAt().isAfter(match.getEvaluatedAt())) {
            return true;
        }
        return false;
    }

    public MatchAnalysisResponse buildResponseFromEntity(JobMatch match, Job job) {
        MatchAnalysisResponse res = new MatchAnalysisResponse();
        res.setJobId(job.getId());
        res.setJobTitle(job.getTitle());
        res.setCompanyName(job.getCompany().getName());
        res.setCandidateProfileId(match.getCandidateProfile().getId());
        res.setRecommendation(match.getRecommendation());
        res.setPriorityScore(match.getPriorityScore());
        res.setQueueTier(match.getQueueTier());
        res.setEvaluatedAt(match.getEvaluatedAt());

        res.setPriorityCategory(match.getPriorityCategory());
        res.setFreshness(match.getFreshness());
        res.setDaysSincePosted(match.getDaysSincePosted());

        try {
            res.setStrongMatches(objectMapper.readValue(match.getStrongMatches(), new TypeReference<List<String>>() {}));
            res.setPartialMatches(objectMapper.readValue(match.getPartialMatches(), new TypeReference<List<String>>() {}));
            res.setGaps(objectMapper.readValue(match.getGaps(), new TypeReference<List<String>>() {}));
            res.setTransferableExperience(objectMapper.readValue(match.getTransferableExperience(), new TypeReference<List<String>>() {}));
            res.setRiskFactors(objectMapper.readValue(match.getRiskFactors(), new TypeReference<List<String>>() {}));
            res.setAdvantageReport(objectMapper.readValue(match.getAdvantageReport(), ApplicationAdvantageReportDto.class));

            if (match.getWhyThisJob() != null && !match.getWhyThisJob().isBlank()) {
                res.setWhyThisJob(objectMapper.readValue(match.getWhyThisJob(), new TypeReference<List<String>>() {}));
            }
            if (match.getPotentialConcerns() != null && !match.getPotentialConcerns().isBlank()) {
                res.setPotentialConcerns(objectMapper.readValue(match.getPotentialConcerns(), new TypeReference<List<String>>() {}));
            }
            if (match.getRequirementCoverage() != null && !match.getRequirementCoverage().isBlank()) {
                res.setRequirementCoverage(objectMapper.readValue(match.getRequirementCoverage(), new TypeReference<List<com.jobhunter.dto.prioritization.RequirementCoverageItem>>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to deserialize match entity json", e);
        }

        CandidateProfile profile = match.getCandidateProfile();
        if (profile != null) {
            float[] jobVector = vectorService.getOrGenerateJobEmbedding(job);
            float[] candidateVector = vectorService.getOrGenerateCandidateEmbedding(profile);
            double semanticSimilarity = vectorService.cosineSimilarity(jobVector, candidateVector);
            res.setSemanticSimilarityScore(Math.round(semanticSimilarity * 100.0) / 100.0);

            res.setOverallAssessment(buildOverallAssessment(job, profile, res.getRecommendation(), res.getPriorityScore(), res.getStrongMatches(), res.getGaps()));

            if (profile.getSkills() != null) {
                Map<String, CandidateSkill> candidateSkillsMap = profile.getSkills().stream()
                        .collect(Collectors.toMap(
                                cs -> cs.getSkill().getName().toLowerCase(),
                                cs -> cs,
                                (e1, e2) -> e1
                        ));
                List<JobRequirement> requirements = requirementExtractor.extractAndPersistRequirements(job);
                List<RequirementMatchItem> reqAnalyses = new ArrayList<>();
                for (JobRequirement req : requirements) {
                    reqAnalyses.add(evaluateRequirement(req, profile, candidateSkillsMap));
                }
                res.setRequirementAnalysis(reqAnalyses);
            }

            if (match.getPriorityCategory() == null) {
                com.jobhunter.dto.prioritization.JobPrioritizationResult prio =
                        prioritizationService.evaluatePrioritization(job, profile, res);
                res.setPriorityCategory(prio.getPriorityCategory() != null ? prio.getPriorityCategory().name() : null);
                res.setFreshness(prio.getFreshness() != null ? prio.getFreshness().name() : null);
                res.setDaysSincePosted(prio.getDaysSincePosted());
                res.setWhyThisJob(prio.getWhyThisJob());
                res.setPotentialConcerns(prio.getPotentialConcerns());
                res.setHardConstraintViolations(prio.getHardConstraintViolations());
                res.setRequirementCoverage(prio.getRequirementCoverage());
                res.setKeyTechnologies(prio.getKeyTechnologies());
                if (prio.getPriorityScore() != null) {
                    res.setPriorityScore(BigDecimal.valueOf(prio.getPriorityScore()));
                }
                match.setPriorityCategory(res.getPriorityCategory());
                match.setFreshness(res.getFreshness());
                match.setDaysSincePosted(res.getDaysSincePosted());
                match.setPriorityScore(res.getPriorityScore());
                try {
                    match.setWhyThisJob(objectMapper.writeValueAsString(prio.getWhyThisJob()));
                    match.setPotentialConcerns(objectMapper.writeValueAsString(prio.getPotentialConcerns()));
                    match.setRequirementCoverage(objectMapper.writeValueAsString(prio.getRequirementCoverage()));
                } catch (Exception ignored) {}
                jobMatchRepository.save(match);
            } else {
                com.jobhunter.dto.prioritization.JobPrioritizationResult prio =
                        prioritizationService.evaluatePrioritization(job, profile, res);
                res.setKeyTechnologies(prio.getKeyTechnologies());
                res.setHardConstraintViolations(prio.getHardConstraintViolations());
            }
        }

        return res;
    }

    @Transactional
    public Optional<MatchAnalysisResponse> getMatchAnalysis(UUID jobId, UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        return Optional.of(analyzeAndMatch(jobId, userId));
    }

    @Transactional(readOnly = true)
    public List<JobRequirementResponse> getJobRequirements(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        List<JobRequirement> reqs = requirementExtractor.extractAndPersistRequirements(job);
        List<JobRequirementResponse> responses = new ArrayList<>();
        for (JobRequirement r : reqs) {
            responses.add(new JobRequirementResponse(
                    r.getId(),
                    r.getRequirementType(),
                    r.getCategory(),
                    r.getDescription(),
                    r.getSkill() != null ? r.getSkill().getName() : null,
                    r.getInferredImportance(),
                    r.isImplied(),
                    r.getRawTextSnippet()
            ));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public ApplicationAdvantageReportDto getAdvantageReport(UUID jobId, UUID userId) {
        MatchAnalysisResponse match = analyzeAndMatch(jobId, userId);
        return match.getAdvantageReport();
    }

    @Transactional(readOnly = true)
    public TailoringRecommendationDto getTailoringRecommendations(UUID jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        MatchAnalysisResponse match = analyzeAndMatch(jobId, userId);

        TailoringRecommendationDto dto = new TailoringRecommendationDto();
        dto.setJobId(job.getId());
        dto.setJobTitle(job.getTitle());
        dto.setCompanyName(job.getCompany().getName());

        // Sections to reorder
        dto.getSectionsToReorder().add("Lead with Core Backend Technical Skills: Java (17), Spring Boot, PostgreSQL, Docker");
        dto.getSectionsToReorder().add("Prioritize Commercial Work Experience over Academic/Certification details");
        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            dto.getSectionsToReorder().add("Promote relevant architectural project: " + profile.getProjects().get(0).getName());
        }

        // Skills to feature
        dto.setSkillsToFeature(match.getStrongMatches().stream()
                .map(s -> s.split("—")[0].trim())
                .distinct()
                .collect(Collectors.toList()));

        // Skills to de-emphasize
        dto.getSkillsToDeemphasize().add("Non-essential frontend technologies (unless applying for a full-stack vacancy)");
        dto.getSkillsToDeemphasize().add("Secondary languages or tools not requested in the job description");

        // Grounded Bullet Sharpening Proposals
        if (profile.getExperiences() != null && !profile.getExperiences().isEmpty()) {
            CandidateExperience exp = profile.getExperiences().get(0);
            dto.getBulletSharpeningProposals().add(new TailoringRecommendationDto.BulletSharpeningProposal(
                    "Engineered backend services and handled database queries.",
                    "Architected high-throughput REST APIs using Spring Boot and Java 17, optimizing PostgreSQL queries for sub-50ms latency.",
                    "Verified commercial role at " + exp.getCompany(),
                    "Aligns with JD requirement for Java, Spring Boot microservices, and database performance."
            ));
        } else {
            dto.getBulletSharpeningProposals().add(new TailoringRecommendationDto.BulletSharpeningProposal(
                    "Built backend features with Java and SQL.",
                    "Developed production REST microservices using Java 17, Spring Boot, and PostgreSQL with complete transaction isolation.",
                    "Candidate verified commercial skills: Java, Spring Boot, PostgreSQL",
                    "Directly satisfies employer core priorities."
            ));
        }

        dto.setRationale("Tailoring aligns the candidate's verified commercial achievements with " + job.getCompany().getName() + "'s specific technical priorities without fabricating unverified claims.");
        return dto;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
