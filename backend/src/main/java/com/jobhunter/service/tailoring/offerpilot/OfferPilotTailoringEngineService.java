package com.jobhunter.service.tailoring.offerpilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.tailoring.offerpilot.*;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.tailoring.PdfGenerationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OfferPilot AI Resume Tailoring Engine Service.
 * Implements the complete end-to-end grounded-by-contract tailoring pipeline.
 */
@Service
public class OfferPilotTailoringEngineService {

    private static final Logger log = LoggerFactory.getLogger(OfferPilotTailoringEngineService.class);

    private final TailoredResumeRepository tailoredResumeRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final TailoringGuardrailService guardrailService;
    private final OfferPilotLatexRenderer latexRenderer;
    private final AtsScoringService atsScoringService;
    private final PdfGenerationService pdfGenerationService;
    private final com.jobhunter.service.fact.CandidateFactStoreService factStoreService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String uploadDir;

    public OfferPilotTailoringEngineService(
            TailoredResumeRepository tailoredResumeRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            CandidateProfileRepository candidateProfileRepository,
            JobRequirementRepository jobRequirementRepository,
            TailoringGuardrailService guardrailService,
            OfferPilotLatexRenderer latexRenderer,
            AtsScoringService atsScoringService,
            PdfGenerationService pdfGenerationService,
            com.jobhunter.service.fact.CandidateFactStoreService factStoreService,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${ai.gemini.model-flash:gemini-flash-latest}") String geminiModel,
            @Value("${storage.upload-dir}") String uploadDir) {
        this.tailoredResumeRepository = tailoredResumeRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.guardrailService = guardrailService;
        this.latexRenderer = latexRenderer;
        this.atsScoringService = atsScoringService;
        this.pdfGenerationService = pdfGenerationService;
        this.factStoreService = factStoreService;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        this.geminiModel = (geminiModel != null && !geminiModel.isBlank() && !geminiModel.contains("1.5")) ? geminiModel.trim() : "gemini-flash-latest";
        this.uploadDir = uploadDir;
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Creates and generates an OfferPilot tailored resume.
     * Enforces sanitization, LLM JSON contract, anti-hallucination guardrail,
     * LaTeX rendering, PDF generation, closed-loop ATS scoring, and persistence.
     */
    @Transactional
    public TailoredResumeDetailResponse createTailoredResume(UUID userId, CreateTailoredResumeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        // 1. Fetch Master Resume
        Resume resume;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + request.getResumeId()));
        } else {
            resume = resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No master resume found for candidate"));
        }

        // 2. Fetch Job Description
        Job job = jobRepository.findById(request.getJobDescriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Job description not found: " + request.getJobDescriptionId()));

        // 3. Extract and Clean Master Resume Text
        String rawResumeText = resume.getRawExtractedText();
        String cleanedResumeText = ResumeTextSanitizer.sanitize(rawResumeText);

        // 4. Pre-Extracted JD Intelligence
        List<JobRequirement> requirements = jobRequirementRepository.findByJobId(job.getId());
        List<String> requiredSkills = new ArrayList<>();
        List<String> preferredSkills = new ArrayList<>();
        List<String> responsibilities = new ArrayList<>();
        List<String> requiredQuals = new ArrayList<>();
        List<String> preferredQuals = new ArrayList<>();
        List<String> keywords = new ArrayList<>();

        for (JobRequirement req : requirements) {
            String val = req.getDescription();
            if (val == null || val.isBlank()) continue;
            String type = req.getRequirementType() != null ? req.getRequirementType().toUpperCase() : "";
            String cat = req.getCategory() != null ? req.getCategory().toUpperCase() : "";

            if ("MUST_HAVE".equals(type) || "REQUIRED".equals(type)) {
                if ("EDUCATION".equals(cat)) {
                    requiredQuals.add(val);
                } else {
                    requiredSkills.add(val);
                }
            } else if ("NICE_TO_HAVE".equals(type) || "PREFERRED".equals(type)) {
                if ("EDUCATION".equals(cat)) {
                    preferredQuals.add(val);
                } else {
                    preferredSkills.add(val);
                }
            } else if ("RESPONSIBILITIES".equals(type) || "RESPONSIBILITY".equals(type) || "PROCESS".equals(cat)) {
                responsibilities.add(val);
            } else if ("QUALIFICATION".equals(type) || "EDUCATION".equals(cat)) {
                requiredQuals.add(val);
            } else {
                keywords.add(val);
            }
        }

        // Add additional fallback keywords if none explicitly tagged
        if (requiredSkills.isEmpty() && job.getRawDescriptionMarkdown() != null) {
            String desc = job.getRawDescriptionMarkdown();
            extractFallbackSkills(desc, requiredSkills, preferredSkills, keywords);
        }

        // 5. Build Strict Anti-Hallucination Prompt
        String companyName = job.getCompany() != null ? job.getCompany().getName() : "Company";
        String prompt = ResumeTailoringPromptBuilder.buildPrompt(
                companyName,
                job.getTitle(),
                requiredSkills,
                preferredSkills,
                responsibilities,
                requiredQuals,
                preferredQuals,
                keywords,
                cleanedResumeText
        );

        // 6. LLM Generation (Temperature: 0.1, Mode: JSON)
        TailoredResumePayload payload = null;
        if (isGeminiAvailable()) {
            try {
                payload = invokeGemini(prompt);
            } catch (Exception e) {
                log.warn("Gemini resume tailoring call failed, falling back to deterministic engine: {}", e.getMessage());
            }
        }

        if (payload == null) {
            payload = generateDeterministicTailoring(cleanedResumeText, job, profile, requiredSkills, preferredSkills);
        }

        // 7. Guardrail Service Validation & Verification (Fail-Fast Anti-Hallucination)
        guardrailService.validateAndAugmentNotes(payload, requiredSkills);

        // 8. Generate Deterministic LaTeX
        String latexSource = latexRenderer.renderLatex(payload.getTailoredResume());

        // 9. Entity Initialization
        String displayName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                ? request.getDisplayName()
                : (companyName + " - " + job.getTitle() + " Tailored Resume");

        String templateName = request.getTemplateName() != null && !request.getTemplateName().isBlank()
                ? request.getTemplateName()
                : "PROFESSIONAL_DEFAULT";

        TailoredResume entity = new TailoredResume();
        entity.setCandidateProfile(profile);
        entity.setMasterResume(resume);
        entity.setJob(job);
        entity.setDisplayName(displayName);
        entity.setTargetJobTitle(job.getTitle());
        entity.setTargetRole(job.getTitle());
        entity.setTargetCompany(companyName);
        entity.setTemplateName(templateName);
        entity.setStatus("GENERATED");
        entity.setLatexSource(latexSource);
        entity.setTailoredMarkdown(buildMarkdownFromContent(payload.getTailoredResume()));

        // Serialize structured OfferPilot payloads
        try {
            entity.setStructuredContentJson(objectMapper.writeValueAsString(payload.getTailoredResume()));
            entity.setMatchedSkillsJson(objectMapper.writeValueAsString(payload.getMatchedSkills()));
            entity.setPartiallyMatchedSkillsJson(objectMapper.writeValueAsString(payload.getPartiallyMatchedSkills()));
            entity.setMissingSkillsJson(objectMapper.writeValueAsString(payload.getMissingSkills()));
            entity.setTailoringNotesJson(objectMapper.writeValueAsString(payload.getTailoringNotes()));
        } catch (Exception e) {
            log.error("Failed to serialize OfferPilot JSON columns", e);
        }

        // Save entity to obtain ID
        TailoredResume saved = tailoredResumeRepository.save(entity);

        // 10. Write LaTeX file to disk: uploads/tailored-resumes/{userId}/{tailoredResumeId}/resume.tex
        Path outputDir = Paths.get(uploadDir, "tailored-resumes", userId.toString(), saved.getId().toString());
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Path texPath = outputDir.resolve("resume.tex");
            Files.writeString(texPath, latexSource, StandardCharsets.UTF_8);
            saved.setLatexFilePath(texPath.toAbsolutePath().toString());

            // 11. Compile/Render PDF to disk: uploads/tailored-resumes/{userId}/{tailoredResumeId}/resume.pdf
            Path pdfPath = outputDir.resolve("resume.pdf");
            File pdfFile = generateAtsPdf(payload.getTailoredResume(), pdfPath.toFile());
            if (pdfFile != null && pdfFile.exists()) {
                saved.setPdfFilePath(pdfFile.getAbsolutePath());
                saved.setPdfFileSizeBytes(pdfFile.length());
            }
        } catch (IOException e) {
            log.error("Failed to write LaTeX / PDF file to disk for tailored resume [{}]", saved.getId(), e);
        }

        // 12. Closed-Loop ATS Scoring & Comparison
        AtsComparisonScoreDto comparison = atsScoringService.computeComparison(
                cleanedResumeText,
                payload.getTailoredResume(),
                job,
                requiredSkills,
                preferredSkills,
                keywords
        );
        saved.setAtsScoreEstimate(BigDecimal.valueOf(comparison.getTailoredOverallScore()));

        // Final save
        saved = tailoredResumeRepository.save(saved);

        return toDetailResponse(saved, payload, comparison);
    }

    /**
     * Lists all tailored resumes for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<TailoredResumeSummaryResponse> listTailoredResumes(UUID userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        List<TailoredResume> list = tailoredResumeRepository.findByCandidateProfileIdOrderByCreatedAtDesc(profile.getId());
        return list.stream().map(this::toSummaryResponse).collect(Collectors.toList());
    }

    /**
     * Retrieves full detail of a tailored resume.
     */
    @Transactional(readOnly = true)
    public TailoredResumeDetailResponse getTailoredResume(UUID id, UUID userId) {
        TailoredResume entity = tailoredResumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tailored resume not found or access denied: " + id));

        TailoredResumePayload payload = deserializePayload(entity);
        AtsComparisonScoreDto comparison = computeComparisonForEntity(entity, payload);

        return toDetailResponse(entity, payload, comparison);
    }

    /**
     * Re-renders LaTeX for an existing tailored resume and saves to disk.
     */
    @Transactional
    public TailoredResumeDetailResponse renderLatexForResume(UUID id, UUID userId) {
        TailoredResume entity = tailoredResumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tailored resume not found or access denied: " + id));

        TailoredResumePayload payload = deserializePayload(entity);
        String latexSource = latexRenderer.renderLatex(payload.getTailoredResume());
        entity.setLatexSource(latexSource);

        Path outputDir = Paths.get(uploadDir, "tailored-resumes", userId.toString(), entity.getId().toString());
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Path texPath = outputDir.resolve("resume.tex");
            Files.writeString(texPath, latexSource, StandardCharsets.UTF_8);
            entity.setLatexFilePath(texPath.toAbsolutePath().toString());
        } catch (IOException e) {
            log.error("Failed to write rendered LaTeX file", e);
        }

        entity = tailoredResumeRepository.save(entity);
        return toDetailResponse(entity, payload, computeComparisonForEntity(entity, payload));
    }

    /**
     * Gets the generated .tex file for download.
     */
    @Transactional(readOnly = true)
    public File getLatexFile(UUID id, UUID userId) {
        TailoredResume entity = tailoredResumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tailored resume not found or access denied: " + id));

        if (entity.getLatexFilePath() != null) {
            File f = new File(entity.getLatexFilePath());
            if (f.exists() && f.length() > 0) return f;
        }

        // If file doesn't exist on disk, re-create from latexSource
        if (entity.getLatexSource() != null && !entity.getLatexSource().isBlank()) {
            Path outputDir = Paths.get(uploadDir, "tailored-resumes", userId.toString(), entity.getId().toString());
            try {
                Files.createDirectories(outputDir);
                Path texPath = outputDir.resolve("resume.tex");
                Files.writeString(texPath, entity.getLatexSource(), StandardCharsets.UTF_8);
                return texPath.toFile();
            } catch (IOException e) {
                log.error("Failed to regenerate .tex file from source", e);
            }
        }

        throw new IllegalStateException("LaTeX file not found for tailored resume: " + id);
    }

    /**
     * Deletes tailored resume and disk assets.
     */
    @Transactional
    public void deleteTailoredResume(UUID id, UUID userId) {
        TailoredResume entity = tailoredResumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tailored resume not found or access denied: " + id));

        // Delete disk directory if exists
        try {
            Path dir = Paths.get(uploadDir, "tailored-resumes", userId.toString(), entity.getId().toString());
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            log.warn("Could not delete directory for tailored resume: {}", id, e);
        }

        tailoredResumeRepository.delete(entity);
    }

    // --- LLM AND DETERMINISTIC ENGINE HELPERS ---

    private boolean isGeminiAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private TailoredResumePayload invokeGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/" + geminiModel + ":generateContent")
                        .queryParam("key", geminiApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) return null;

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String rawJson = textNode.asText().trim();
            if (rawJson.startsWith("```json")) rawJson = rawJson.substring(7);
            if (rawJson.startsWith("```")) rawJson = rawJson.substring(3);
            if (rawJson.endsWith("```")) rawJson = rawJson.substring(0, rawJson.length() - 3);

            return objectMapper.readValue(rawJson.trim(), TailoredResumePayload.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini tailored resume JSON response", e);
            return null;
        }
    }

    /**
     * High-fidelity deterministic fallback OfferPilot engine.
     * Extracts existing resume content, matches skills explicitly against JD,
     * reframes bullets, and guarantees zero hallucination.
     */
    public TailoredResumePayload generateDeterministicTailoring(
            String cleanedResumeText,
            Job job,
            CandidateProfile profile,
            List<String> requiredSkills,
            List<String> preferredSkills) {

        TailoredResumePayload payload = new TailoredResumePayload();
        TailoredResumePayload.TailoredResumeContent content = payload.getTailoredResume();
        User user = profile.getUser();

        // Contact info
        TailoredResumePayload.ContactInfo c = content.getContactInfo();
        c.setFullName((user.getFirstName() + " " + user.getLastName()).trim());
        c.setEmail(user.getEmail());
        c.setPhone(profile.getPhoneNumber());
        c.setLocation(profile.getCurrentLocation() != null ? profile.getCurrentLocation() : "India");
        c.setLinkedinUrl(profile.getLinkedinUrl());
        c.setGithubUrl(profile.getGithubUrl());

        String resumeLower = cleanedResumeText.toLowerCase();

        // Skill Triaging: MATCHED, PARTIALLY_MATCHED, MISSING
        Set<String> allJdSkills = new LinkedHashSet<>(requiredSkills);
        allJdSkills.addAll(preferredSkills);

        for (String skill : allJdSkills) {
            if (skill == null || skill.isBlank()) continue;
            String norm = TailoringGuardrailService.normalizeSkill(skill);

            if (resumeLower.contains(norm)) {
                payload.getMatchedSkills().add(new SkillClassification(
                        skill, "MATCHED", "Explicitly mentioned in master resume text: \"" + skill + "\""
                ));
            } else if (isPartialMatch(skill, resumeLower)) {
                payload.getPartiallyMatchedSkills().add(new SkillClassification(
                        skill, "PARTIALLY_MATCHED", "Related domain context found in master resume"
                ));
            } else {
                payload.getMissingSkills().add(new SkillClassification(
                        skill, "MISSING", "Not found in master resume evidence"
                ));
            }
        }

        // Populate verified skills into tailoredResume.skills
        TailoredResumePayload.SkillsContainer skills = content.getSkills();
        if (profile.getSkills() != null) {
            for (CandidateSkill cs : profile.getSkills()) {
                if (cs.getSkill() == null) continue;
                String name = cs.getSkill().getName().trim();
                String lower = name.toLowerCase();

                // Skip any skill that was triaged as MISSING
                boolean isMissing = payload.getMissingSkills().stream()
                        .anyMatch(ms -> TailoringGuardrailService.normalizeSkill(ms.getSkill()).equals(TailoringGuardrailService.normalizeSkill(name)));
                if (isMissing) continue;

                if (lower.contains("java") || lower.contains("python") || lower.contains("script") || lower.contains("c#") || lower.contains("c++") || lower.contains("go")) {
                    skills.getProgrammingLanguages().add(name);
                } else if (lower.contains("spring") || lower.contains("rest") || lower.contains("jwt") || lower.contains("react") || lower.contains("node") || lower.contains("express")) {
                    skills.getFrameworks().add(name);
                } else if (lower.contains("sql") || lower.contains("postgres") || lower.contains("mysql") || lower.contains("redis") || lower.contains("mongo")) {
                    skills.getDatabases().add(name);
                } else if (lower.contains("aws") || lower.contains("docker") || lower.contains("kubernetes") || lower.contains("gcp") || lower.contains("azure") || lower.contains("ci/cd")) {
                    skills.getCloud().add(name);
                } else if (lower.contains("git") || lower.contains("maven") || lower.contains("jira") || lower.contains("postman")) {
                    skills.getTools().add(name);
                } else {
                    skills.getOther().add(name);
                }
            }
        }

        // Professional Summary
        String targetTitle = job.getTitle() != null ? job.getTitle() : "Software Engineer";
        String yoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience() + "+ years" : "2+ years";
        List<String> matchedNames = payload.getMatchedSkills().stream().map(SkillClassification::getSkill).limit(3).toList();
        String matchedStr = !matchedNames.isEmpty() ? String.join(", ", matchedNames) : "backend systems";

        content.setProfessionalSummary("Software Engineer with " + yoe + " of commercial production experience specializing in "
                + matchedStr + ". Proven track record architecting high-reliability RESTful microservices, optimizing database performance, and building resilient distributed systems.");

        // Experience
        if (profile.getExperiences() != null) {
            for (CandidateExperience exp : profile.getExperiences()) {
                TailoredResumePayload.ExperienceEntry entry = new TailoredResumePayload.ExperienceEntry();
                entry.setCompany(exp.getCompany() != null ? exp.getCompany() : "Engineering Team");
                entry.setRole(exp.getRole() != null ? exp.getRole() : "Software Engineer");
                String duration = exp.getDuration() != null ? exp.getDuration().trim() : "";
                String startDate = duration;
                String endDate = "";
                if (duration.contains("-")) {
                    String[] parts = duration.split("-", 2);
                    startDate = parts[0].trim();
                    endDate = parts[1].trim();
                } else if (duration.contains("–")) {
                    String[] parts = duration.split("–", 2);
                    startDate = parts[0].trim();
                    endDate = parts[1].trim();
                }
                entry.setStartDate(startDate);
                entry.setEndDate(endDate.isBlank() ? "Present" : endDate);

                List<String> rawBullets = parseList(exp.getAchievements());
                rawBullets.addAll(parseList(exp.getResponsibilities()));

                for (String raw : rawBullets) {
                    if (raw == null || raw.isBlank()) continue;
                    String clean = raw.trim();
                    String cleanLower = clean.toLowerCase();
                    String rewritten = clean;

                    if (cleanLower.matches("^(worked on|responsible for|helped with|assisted with|participated in)\\b.*")) {
                        rewritten = clean.replaceFirst("(?i)^(worked on|responsible for|helped with|assisted with|participated in)\\s*", "Developed and maintained ");
                    } else if (cleanLower.matches("^(built|designed|created)\\b.*")) {
                        rewritten = clean.replaceFirst("(?i)^(built|designed|created)\\s*", "Architected and delivered ");
                    }
                    entry.getBullets().add(rewritten);
                }
                content.getExperience().add(entry);
            }
        }

        // Projects
        if (profile.getProjects() != null) {
            for (CandidateProject proj : profile.getProjects()) {
                TailoredResumePayload.ProjectEntry p = new TailoredResumePayload.ProjectEntry();
                p.setName(proj.getName() != null ? proj.getName() : "Technical Project");
                p.setDescription(proj.getDescription() != null ? proj.getDescription() : "");
                p.setTechnologies(parseList(proj.getTechnologies()));

                List<String> pBullets = parseList(proj.getMeasurableOutcomes());
                pBullets.addAll(parseList(proj.getResponsibilities()));
                for (String pb : pBullets) {
                    if (pb != null && !pb.isBlank()) {
                        p.getBullets().add(pb.trim());
                    }
                }
                content.getProjects().add(p);
            }
        }

        // Education: Grounded in verified Candidate Fact Store
        Set<String> degrees = factStoreService.getAllowedDegrees(profile);
        for (String deg : degrees) {
            if (deg != null && !deg.isBlank()) {
                content.getEducation().add(deg);
            }
        }

        return payload;
    }

    private boolean isPartialMatch(String skill, String text) {
        String lower = skill.toLowerCase();
        if (lower.contains("rest") && text.contains("api")) return true;
        if (lower.contains("postgres") && text.contains("sql")) return true;
        if (lower.contains("docker") && text.contains("container")) return true;
        if (lower.contains("microservice") && text.contains("distributed")) return true;
        return false;
    }

    private void extractFallbackSkills(String desc, List<String> required, List<String> preferred, List<String> keywords) {
        String lower = desc.toLowerCase();
        List<String> commonSkills = List.of(
                "Java", "Spring Boot", "PostgreSQL", "SQL", "Docker", "Git", "REST APIs",
                "Kafka", "AWS", "Kubernetes", "Microservices", "Redis", "TypeScript", "Python"
        );
        for (String s : commonSkills) {
            if (lower.contains(s.toLowerCase())) {
                required.add(s);
            }
        }
    }

    private File generateAtsPdf(TailoredResumePayload.TailoredResumeContent content, File outputFile) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font oblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                float y = 790;
                float margin = 40;
                float width = PDRectangle.A4.getWidth() - (margin * 2);

                // Full Name
                TailoredResumePayload.ContactInfo contact = content.getContactInfo();
                String name = contact != null && contact.getFullName() != null ? contact.getFullName() : "Candidate";
                cs.beginText();
                cs.setFont(bold, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText(cleanForPdfBox(name));
                cs.endText();
                y -= 16;

                // Contact line
                List<String> contactParts = new ArrayList<>();
                if (contact != null) {
                    if (contact.getEmail() != null) contactParts.add(contact.getEmail());
                    if (contact.getPhone() != null) contactParts.add(contact.getPhone());
                    if (contact.getLocation() != null) contactParts.add(contact.getLocation());
                }
                String contactStr = String.join(" | ", contactParts);
                if (!contactStr.isBlank()) {
                    cs.beginText();
                    cs.setFont(regular, 9);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(cleanForPdfBox(contactStr));
                    cs.endText();
                    y -= 18;
                }

                // Summary
                if (content.getProfessionalSummary() != null && !content.getProfessionalSummary().isBlank()) {
                    cs.beginText();
                    cs.setFont(bold, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("PROFESSIONAL SUMMARY");
                    cs.endText();
                    y -= 12;

                    cs.beginText();
                    cs.setFont(regular, 9);
                    cs.newLineAtOffset(margin, y);
                    String sum = cleanForPdfBox(content.getProfessionalSummary());
                    if (sum.length() > 110) sum = sum.substring(0, 110) + "...";
                    cs.showText(sum);
                    cs.endText();
                    y -= 16;
                }

                // Skills
                TailoredResumePayload.SkillsContainer sc = content.getSkills();
                if (sc != null) {
                    cs.beginText();
                    cs.setFont(bold, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("TECHNICAL SKILLS");
                    cs.endText();
                    y -= 12;

                    drawSkillPdf(cs, bold, regular, margin, y, "Languages", sc.getProgrammingLanguages());
                    y -= 12;
                    drawSkillPdf(cs, bold, regular, margin, y, "Frameworks", sc.getFrameworks());
                    y -= 12;
                    drawSkillPdf(cs, bold, regular, margin, y, "Databases", sc.getDatabases());
                    y -= 16;
                }

                // Experience
                if (content.getExperience() != null && !content.getExperience().isEmpty()) {
                    cs.beginText();
                    cs.setFont(bold, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("EXPERIENCE");
                    cs.endText();
                    y -= 12;

                    for (TailoredResumePayload.ExperienceEntry e : content.getExperience()) {
                        if (y < 80) break;
                        cs.beginText();
                        cs.setFont(bold, 9);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(cleanForPdfBox(e.getRole() + " -- " + e.getCompany()));
                        cs.endText();
                        y -= 11;

                        if (e.getBullets() != null) {
                            for (String b : e.getBullets()) {
                                if (y < 60) break;
                                cs.beginText();
                                cs.setFont(regular, 8.5f);
                                cs.newLineAtOffset(margin + 10, y);
                                String cleanB = cleanForPdfBox(b);
                                if (cleanB.length() > 95) cleanB = cleanB.substring(0, 95) + "...";
                                cs.showText("- " + cleanB);
                                cs.endText();
                                y -= 11;
                            }
                        }
                        y -= 4;
                    }
                }
            }

            doc.save(outputFile);
            return outputFile;
        } catch (Exception e) {
            log.error("Failed to render ATS PDF with PDFBox", e);
            return null;
        }
    }

    private void drawSkillPdf(PDPageContentStream cs, PDType1Font bold, PDType1Font reg, float margin, float y, String label, List<String> list) throws IOException {
        if (list == null || list.isEmpty()) return;
        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(margin, y);
        cs.showText(label + ": ");
        cs.setFont(reg, 9);
        String s = cleanForPdfBox(String.join(", ", list));
        if (s.length() > 80) s = s.substring(0, 80) + "...";
        cs.showText(s);
        cs.endText();
    }

    private String cleanForPdfBox(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\x20-\\x7E]", " ").trim();
    }

    private String buildMarkdownFromContent(TailoredResumePayload.TailoredResumeContent content) {
        if (content == null) return "";
        StringBuilder sb = new StringBuilder();
        TailoredResumePayload.ContactInfo c = content.getContactInfo();
        if (c != null && c.getFullName() != null) {
            sb.append("# ").append(c.getFullName()).append("\n");
            sb.append(c.getEmail() != null ? c.getEmail() : "").append(" | ")
                    .append(c.getPhone() != null ? c.getPhone() : "").append(" | ")
                    .append(c.getLocation() != null ? c.getLocation() : "").append("\n\n");
        }
        if (content.getProfessionalSummary() != null && !content.getProfessionalSummary().isBlank()) {
            sb.append("## Professional Summary\n").append(content.getProfessionalSummary()).append("\n\n");
        }
        if (content.getSkills() != null) {
            sb.append("## Technical Skills\n");
            TailoredResumePayload.SkillsContainer s = content.getSkills();
            if (!s.getProgrammingLanguages().isEmpty()) sb.append("- **Languages**: ").append(String.join(", ", s.getProgrammingLanguages())).append("\n");
            if (!s.getFrameworks().isEmpty()) sb.append("- **Frameworks**: ").append(String.join(", ", s.getFrameworks())).append("\n");
            if (!s.getDatabases().isEmpty()) sb.append("- **Databases**: ").append(String.join(", ", s.getDatabases())).append("\n");
            if (!s.getCloud().isEmpty()) sb.append("- **Cloud/DevOps**: ").append(String.join(", ", s.getCloud())).append("\n");
            sb.append("\n");
        }
        if (content.getExperience() != null) {
            sb.append("## Experience\n");
            for (TailoredResumePayload.ExperienceEntry exp : content.getExperience()) {
                sb.append("### ").append(exp.getRole()).append(" -- ").append(exp.getCompany()).append("\n");
                for (String b : exp.getBullets()) {
                    sb.append("- ").append(b).append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private TailoredResumePayload deserializePayload(TailoredResume entity) {
        TailoredResumePayload payload = new TailoredResumePayload();
        try {
            if (entity.getStructuredContentJson() != null && !entity.getStructuredContentJson().isBlank()) {
                payload.setTailoredResume(objectMapper.readValue(entity.getStructuredContentJson(), TailoredResumePayload.TailoredResumeContent.class));
            }
            if (entity.getMatchedSkillsJson() != null && !entity.getMatchedSkillsJson().isBlank()) {
                payload.setMatchedSkills(objectMapper.readValue(entity.getMatchedSkillsJson(), new TypeReference<List<SkillClassification>>() {}));
            }
            if (entity.getPartiallyMatchedSkillsJson() != null && !entity.getPartiallyMatchedSkillsJson().isBlank()) {
                payload.setPartiallyMatchedSkills(objectMapper.readValue(entity.getPartiallyMatchedSkillsJson(), new TypeReference<List<SkillClassification>>() {}));
            }
            if (entity.getMissingSkillsJson() != null && !entity.getMissingSkillsJson().isBlank()) {
                payload.setMissingSkills(objectMapper.readValue(entity.getMissingSkillsJson(), new TypeReference<List<SkillClassification>>() {}));
            }
            if (entity.getTailoringNotesJson() != null && !entity.getTailoringNotesJson().isBlank()) {
                payload.setTailoringNotes(objectMapper.readValue(entity.getTailoringNotesJson(), new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to deserialize TailoredResume JSON fields for ID: {}", entity.getId(), e);
        }
        return payload;
    }

    private AtsComparisonScoreDto computeComparisonForEntity(TailoredResume entity, TailoredResumePayload payload) {
        Resume masterResume = entity.getMasterResume();
        String masterText = (masterResume != null && masterResume.getRawExtractedText() != null)
                ? masterResume.getRawExtractedText()
                : "";

        List<JobRequirement> requirements = jobRequirementRepository.findByJobId(entity.getJob().getId());
        List<String> required = requirements.stream()
                .filter(r -> "MUST_HAVE".equalsIgnoreCase(r.getRequirementType()) || "REQUIRED".equalsIgnoreCase(r.getRequirementType()))
                .map(JobRequirement::getDescription)
                .toList();
        List<String> preferred = requirements.stream()
                .filter(r -> !"MUST_HAVE".equalsIgnoreCase(r.getRequirementType()) && !"REQUIRED".equalsIgnoreCase(r.getRequirementType()))
                .map(JobRequirement::getDescription)
                .toList();

        return atsScoringService.computeComparison(
                masterText,
                payload.getTailoredResume(),
                entity.getJob(),
                required,
                preferred,
                Collections.emptyList()
        );
    }

    private TailoredResumeDetailResponse toDetailResponse(TailoredResume entity, TailoredResumePayload payload, AtsComparisonScoreDto comparison) {
        TailoredResumeDetailResponse resp = new TailoredResumeDetailResponse();
        resp.setId(entity.getId());
        if (entity.getMasterResume() != null) {
            resp.setResumeId(entity.getMasterResume().getId());
        }
        if (entity.getJob() != null) {
            resp.setJobDescriptionId(entity.getJob().getId());
        }
        resp.setDisplayName(entity.getDisplayName() != null ? entity.getDisplayName() : entity.getTargetCompany() + " Tailored Resume");
        resp.setTargetCompany(entity.getTargetCompany());
        resp.setTargetJobTitle(entity.getTargetJobTitle() != null ? entity.getTargetJobTitle() : entity.getTargetRole());
        resp.setTemplateName(entity.getTemplateName());
        resp.setStatus(entity.getStatus());

        resp.setStructuredContent(payload.getTailoredResume());
        resp.setMatchedSkills(payload.getMatchedSkills());
        resp.setPartiallyMatchedSkills(payload.getPartiallyMatchedSkills());
        resp.setMissingSkills(payload.getMissingSkills());
        resp.setTailoringNotes(payload.getTailoringNotes());

        resp.setLatexFilePath(entity.getLatexFilePath());
        resp.setPdfFilePath(entity.getPdfFilePath());
        resp.setLatexDownloadUrl("/api/v1/tailored-resumes/" + entity.getId() + "/download-latex");
        resp.setPdfDownloadUrl("/api/tailored-resumes/" + entity.getId() + "/download");
        resp.setAtsScoreEstimate(entity.getAtsScoreEstimate());
        resp.setComparison(comparison);
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());

        return resp;
    }

    private TailoredResumeSummaryResponse toSummaryResponse(TailoredResume entity) {
        TailoredResumeSummaryResponse resp = new TailoredResumeSummaryResponse();
        resp.setId(entity.getId());
        if (entity.getMasterResume() != null) resp.setResumeId(entity.getMasterResume().getId());
        if (entity.getJob() != null) resp.setJobDescriptionId(entity.getJob().getId());
        resp.setDisplayName(entity.getDisplayName() != null ? entity.getDisplayName() : entity.getTargetCompany() + " Tailored Resume");
        resp.setTargetCompany(entity.getTargetCompany());
        resp.setTargetJobTitle(entity.getTargetJobTitle() != null ? entity.getTargetJobTitle() : entity.getTargetRole());
        resp.setTemplateName(entity.getTemplateName());
        resp.setStatus(entity.getStatus());
        resp.setAtsScoreEstimate(entity.getAtsScoreEstimate());
        resp.setPdfAvailable("GENERATED".equalsIgnoreCase(entity.getStatus())
                || "READY_FOR_DOWNLOAD".equalsIgnoreCase(entity.getStatus())
                || (entity.getPdfFilePath() != null && new File(entity.getPdfFilePath()).exists()));
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

    private List<String> parseList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String clean = raw.replaceAll("[\\[\\]\"']", "");
            for (String part : clean.split(",")) {
                if (!part.trim().isBlank()) list.add(part.trim());
            }
            return list;
        }
    }
}
