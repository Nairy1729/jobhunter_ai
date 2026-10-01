package com.jobhunter.service.prioritization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.matching.RequirementMatchItem;
import com.jobhunter.dto.prioritization.*;
import com.jobhunter.model.entity.CandidateExperience;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.CandidateProject;
import com.jobhunter.model.entity.CandidateSkill;
import com.jobhunter.model.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for intelligent, evidence-backed job prioritization.
 *
 * Core Product Principle:
 * MATCH answers: "How well does the candidate's background correspond to this job?"
 * PRIORITY answers: "Given the match, job quality, freshness, requirements, and constraints, how much attention should this job receive?"
 *
 * Distinguishes HARD CONSTRAINTS (blocking incompatibilities) from SOFT SIGNALS (nice-to-haves).
 * Never claims hiring probabilities or predicts employer outcomes.
 */
@Service
public class JobPrioritizationService {

    private static final Logger log = LoggerFactory.getLogger(JobPrioritizationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobPrioritizationResult evaluatePrioritization(Job job, CandidateProfile profile, MatchAnalysisResponse match) {
        JobPrioritizationResult result = new JobPrioritizationResult();
        result.setJobId(job.getId());
        result.setCandidateProfileId(profile.getId());

        // 1. Calculate Job Freshness (Strict: Scrape date is NOT posting date)
        evaluateFreshness(job, result);

        // 2. Evaluate Hard Constraints
        List<String> hardViolations = evaluateHardConstraints(job, profile, match);
        result.setHardConstraintViolations(hardViolations);

        // 3. Build Requirement Coverage Audit
        List<RequirementCoverageItem> coverageItems = buildRequirementCoverage(match, profile);
        result.setRequirementCoverage(coverageItems);

        // 4. Build Key Technology Indicators for JobCard
        List<KeyTechCoverageDto> keyTechList = buildKeyTechCoverage(job, profile, coverageItems);
        result.setKeyTechnologies(keyTechList);

        // 5. Generate Evidence-Backed Positives ("Why This Job?")
        List<String> whyThisJob = buildWhyThisJob(job, profile, match, coverageItems);
        result.setWhyThisJob(whyThisJob);

        // 6. Generate Transparent Negative Evidence ("Potential Concerns")
        List<String> potentialConcerns = buildPotentialConcerns(job, profile, match, hardViolations, result.getFreshness(), result.getDaysSincePosted());
        result.setPotentialConcerns(potentialConcerns);

        // 7. Determine Priority Category & Internal Score (NOT a hiring probability)
        determinePriorityCategoryAndScore(match, hardViolations, result.getFreshness(), coverageItems, result);

        log.debug("PRIORITIZATION_COMPLETED - Job: [{}] Category: [{}] Score: [{}] Freshness: [{}]",
                job.getTitle(), result.getPriorityCategory(), result.getPriorityScore(), result.getFreshness());

        return result;
    }

    private void evaluateFreshness(Job job, JobPrioritizationResult result) {
        LocalDate postingDate = job.getPostingDate();
        if (postingDate == null) {
            result.setFreshness(JobFreshness.UNKNOWN);
            result.setDaysSincePosted(null);
            return;
        }

        long days = ChronoUnit.DAYS.between(postingDate, LocalDate.now());
        int daysSincePosted = (int) Math.max(0, days);
        result.setDaysSincePosted(daysSincePosted);

        if (daysSincePosted <= 7) {
            result.setFreshness(JobFreshness.NEW);
        } else if (daysSincePosted <= 30) {
            result.setFreshness(JobFreshness.RECENT);
        } else if (daysSincePosted <= 60) {
            result.setFreshness(JobFreshness.OLDER);
        } else {
            result.setFreshness(JobFreshness.STALE);
        }
    }

    private List<String> evaluateHardConstraints(Job job, CandidateProfile profile, MatchAnalysisResponse match) {
        List<String> violations = new ArrayList<>();
        double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;

        // Constraint A: Seniority / Experience Dramatically Incompatible
        if (job.getMinExperienceYears() != null) {
            double reqYoe = job.getMinExperienceYears().doubleValue();
            if ((reqYoe >= 6.0 && (reqYoe - candYoe >= 2.5)) || (reqYoe >= 5.0 && (reqYoe - candYoe > 2.5))) {
                violations.add("Role explicitly requires " + reqYoe + "+ years experience; candidate verified experience is " + candYoe + " years.");
            }
        }

        // Title-based principal/staff/director barrier check for early-career profile
        String titleLower = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
        if (candYoe < 4.0 && (titleLower.contains("staff") || titleLower.contains("principal") || titleLower.contains("director") || titleLower.contains("vice president"))) {
            violations.add("Role level (" + job.getTitle() + ") is an executive/staff tier significantly exceeding candidate verified seniority.");
        }

        // Constraint B: Mandatory Domain / Non-Transferable Tech Absence
        // E.g. iOS/Mobile role requiring Swift/Objective-C when candidate has zero mobile experience
        boolean isMobileJob = titleLower.contains("ios") || titleLower.contains("android") || titleLower.contains("mobile");
        if (isMobileJob) {
            boolean hasMobileSkill = profile.getSkills() != null && profile.getSkills().stream()
                    .anyMatch(cs -> cs.getSkill() != null &&
                            (cs.getSkill().getName().equalsIgnoreCase("Swift") ||
                             cs.getSkill().getName().equalsIgnoreCase("Kotlin") ||
                             cs.getSkill().getName().equalsIgnoreCase("Objective-C")));
            if (!hasMobileSkill) {
                violations.add("Mandatory core mobile technologies (Swift/Objective-C/Kotlin) are required with no verified candidate evidence.");
            }
        }

        // Constraint C: Explicitly Incompatible Location & Strict On-Site / In-Office Hybrid Work Mode
        List<String> preferredLocations = parseJsonList(profile.getPreferredLocations());
        String rawText = job.getRawDescriptionMarkdown() != null ? job.getRawDescriptionMarkdown().toLowerCase() : "";
        boolean hasOnsiteIndicator = "ON_SITE".equalsIgnoreCase(job.getWorkMode())
                || rawText.contains("location type: on-site")
                || rawText.contains("location type\n\non-site")
                || rawText.contains("onsite 3 days")
                || rawText.contains("in-office three days");

        if (hasOnsiteIndicator) {
            String jobLoc = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
            boolean matchesPrefLoc = preferredLocations.stream().anyMatch(pl ->
                    !pl.isBlank() && !pl.equalsIgnoreCase("Remote") && !pl.equalsIgnoreCase("International")
                    && (jobLoc.contains(pl.toLowerCase()) || rawText.contains(pl.toLowerCase())));

            if (!matchesPrefLoc) {
                if (rawText.contains("cambridge, ma") || rawText.contains("bellevue, wa") || rawText.contains("new york, ny")
                        || rawText.contains("san francisco") || rawText.contains("tokyo") || rawText.contains("london")
                        || "ON_SITE".equalsIgnoreCase(job.getWorkMode())) {
                    violations.add("Role requires physical in-office/on-site presence in " + (job.getLocation() != null ? job.getLocation() : "target office") + ", which conflicts with candidate location.");
                }
            }
        }

        // Constraint D: Active Government Security Clearance
        boolean requiresClearance = titleLower.contains("security cleared")
                || titleLower.contains("security clearance")
                || rawText.contains("security clearance")
                || rawText.contains("ts/sci")
                || rawText.contains("secret clearance")
                || rawText.contains("polygraph required");
        if (requiresClearance) {
            violations.add("Role explicitly requires active government security clearance, which candidate does not possess.");
        }

        return violations;
    }

    private List<RequirementCoverageItem> buildRequirementCoverage(MatchAnalysisResponse match, CandidateProfile profile) {
        List<RequirementCoverageItem> items = new ArrayList<>();

        if (match.getRequirementAnalysis() != null) {
            for (RequirementMatchItem rmi : match.getRequirementAnalysis()) {
                String importance = rmi.isImplied() ? "PREFERRED" : "MUST_HAVE";
                String evidenceType = determineEvidenceType(rmi.getRequirement(), profile);
                String coverage = rmi.getMatchType() != null ? rmi.getMatchType() : "GAP";
                String explanation = rmi.getExplanation() != null ? rmi.getExplanation() : "";

                items.add(new RequirementCoverageItem(
                        rmi.getRequirement(),
                        importance,
                        evidenceType,
                        coverage,
                        explanation
                ));
            }
        }

        return items;
    }

    private String determineEvidenceType(String requirement, CandidateProfile profile) {
        if (requirement == null || profile == null) return "NONE";
        String reqLower = requirement.toLowerCase();

        // 1. Check commercial work experience
        if (profile.getExperiences() != null) {
            for (CandidateExperience exp : profile.getExperiences()) {
                if ((exp.getEvidenceText() != null && exp.getEvidenceText().toLowerCase().contains(reqLower))
                        || (exp.getResponsibilities() != null && exp.getResponsibilities().toLowerCase().contains(reqLower))
                        || (exp.getTechnologies() != null && exp.getTechnologies().toLowerCase().contains(reqLower))
                        || (exp.getRole() != null && exp.getRole().toLowerCase().contains(reqLower))) {
                    return "COMMERCIAL";
                }
            }
        }

        // 2. Check candidate skills
        if (profile.getSkills() != null) {
            for (CandidateSkill cs : profile.getSkills()) {
                if (cs.getSkill() != null && cs.getSkill().getName().equalsIgnoreCase(requirement)) {
                    if ("COMMERCIAL".equalsIgnoreCase(cs.getExperienceType())) {
                        return "COMMERCIAL";
                    } else if ("PROJECT_ONLY".equalsIgnoreCase(cs.getExperienceType())) {
                        return "PROJECT";
                    } else {
                        return "LEARNING";
                    }
                }
            }
        }

        // 3. Check portfolio projects
        if (profile.getProjects() != null) {
            for (CandidateProject proj : profile.getProjects()) {
                if ((proj.getDescription() != null && proj.getDescription().toLowerCase().contains(reqLower))
                        || (proj.getTechnologies() != null && proj.getTechnologies().toLowerCase().contains(reqLower))
                        || (proj.getEvidenceText() != null && proj.getEvidenceText().toLowerCase().contains(reqLower))
                        || (proj.getName() != null && proj.getName().toLowerCase().contains(reqLower))) {
                    return "PROJECT";
                }
            }
        }

        return "NONE";
    }

    private List<KeyTechCoverageDto> buildKeyTechCoverage(Job job, CandidateProfile profile, List<RequirementCoverageItem> coverageItems) {
        Map<String, KeyTechCoverageDto> techMap = new LinkedHashMap<>();

        // Pull from requirement coverage items first (canonical source)
        for (RequirementCoverageItem rci : coverageItems) {
            String techName = rci.getRequirement();
            if (techName != null && techName.length() < 25 && !techMap.containsKey(techName.toLowerCase())) {
                techMap.put(techName.toLowerCase(), new KeyTechCoverageDto(
                        techName,
                        rci.getCoverage(),
                        rci.getCandidateEvidence()
                ));
            }
            if (techMap.size() >= 5) break;
        }

        return new ArrayList<>(techMap.values());
    }

    private List<String> buildWhyThisJob(Job job, CandidateProfile profile, MatchAnalysisResponse match, List<RequirementCoverageItem> coverage) {
        List<String> positives = new ArrayList<>();

        // 1. Technical alignment from strong matches
        if (match.getStrongMatches() != null) {
            for (String sm : match.getStrongMatches().stream().limit(3).toList()) {
                positives.add("Verified direct experience: " + sm);
            }
        }

        // 2. Experience & Seniority alignment
        double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;
        if (job.getMinExperienceYears() != null && job.getMinExperienceYears().doubleValue() <= candYoe) {
            positives.add("Experience requirement (" + job.getMinExperienceYears() + " yrs) is well-matched to candidate background (" + candYoe + " yrs).");
        } else if (job.getMinExperienceYears() == null) {
            positives.add("Open seniority requirement fits candidate commercial track record (" + candYoe + " yrs).");
        }

        // 3. Location / Work Mode alignment
        List<String> prefLocs = parseJsonList(profile.getPreferredLocations());
        if ("REMOTE".equalsIgnoreCase(job.getWorkMode())) {
            positives.add("Remote work mode fully matches candidate work mode preferences.");
        } else if (job.getLocation() != null && prefLocs.stream().anyMatch(l -> job.getLocation().toLowerCase().contains(l.toLowerCase()))) {
            positives.add("Location (" + job.getLocation() + ") directly matches preferred geographic markets.");
        }

        // 4. Compensation alignment (only when disclosed; never inferred)
        if (job.getMinSalary() != null && profile.getMinSalaryInr() != null) {
            if (job.getMinSalary().compareTo(profile.getMinSalaryInr()) >= 0) {
                positives.add("Disclosed starting compensation meets or exceeds candidate target threshold.");
            }
        }

        return positives;
    }

    private List<String> buildPotentialConcerns(
            Job job,
            CandidateProfile profile,
            MatchAnalysisResponse match,
            List<String> hardViolations,
            JobFreshness freshness,
            Integer daysSincePosted) {

        List<String> concerns = new ArrayList<>();

        // 1. Add all hard constraint violations
        for (String hv : hardViolations) {
            concerns.add("HARD CONSTRAINT: " + hv);
        }

        // 2. Missing preferred or must-have skills
        if (match.getGaps() != null) {
            for (String gap : match.getGaps().stream().limit(3).toList()) {
                concerns.add("Skill gap: " + gap + " is requested without verified candidate evidence.");
            }
        }

        // 3. Partial or project-only exposures
        if (match.getPartialMatches() != null) {
            for (String partial : match.getPartialMatches().stream().limit(2).toList()) {
                concerns.add("Partial evidence: " + partial + " is backed by project exposure rather than commercial track record.");
            }
        }

        // 4. Freshness concerns (without inventing dates)
        if (freshness == JobFreshness.STALE) {
            concerns.add("Stale listing: Posted " + daysSincePosted + " days ago; active hiring pipeline may already be advanced.");
        } else if (freshness == JobFreshness.UNKNOWN) {
            concerns.add("Freshness notice: Employer source did not disclose a verifiable posting date.");
        }

        return concerns;
    }

    private void determinePriorityCategoryAndScore(
            MatchAnalysisResponse match,
            List<String> hardViolations,
            JobFreshness freshness,
            List<RequirementCoverageItem> coverage,
            JobPrioritizationResult result) {

        int baseScore = match.getPriorityScore() != null ? match.getPriorityScore().intValue() : 50;

        // Rule 1: Hard constraint violations cap priority
        if (!hardViolations.isEmpty()) {
            boolean hasSevere = hardViolations.stream().anyMatch(v ->
                    v.contains("Mandatory") ||
                    v.contains("executive/staff tier") ||
                    v.contains("security clearance") ||
                    v.contains("physical in-office/on-site presence") ||
                    v.contains("strict on-site presence"));
            if (hasSevere) {
                result.setPriorityCategory(PriorityCategory.NOT_RECOMMENDED);
                result.setPriorityScore(Math.min(30, Math.max(10, baseScore - 40)));
            } else {
                result.setPriorityCategory(PriorityCategory.LOW_PRIORITY);
                result.setPriorityScore(Math.min(48, Math.max(20, baseScore - 25)));
            }
            return;
        }

        // Rule 2: Evaluate must-have requirement coverage
        long totalMustHaves = coverage.stream().filter(c -> "MUST_HAVE".equals(c.getImportance())).count();
        long strongMustHaves = coverage.stream().filter(c -> "MUST_HAVE".equals(c.getImportance()) && "STRONG".equals(c.getCoverage())).count();
        long partialMustHaves = coverage.stream().filter(c -> "MUST_HAVE".equals(c.getImportance()) && "PARTIAL".equals(c.getCoverage())).count();
        double mustHaveRatio = totalMustHaves > 0 ? (double) strongMustHaves / totalMustHaves : 1.0;
        double effectiveRatio = totalMustHaves > 0 ? (double) (strongMustHaves + 0.5 * partialMustHaves) / totalMustHaves : 1.0;

        // Rule 3: High Priority conditions
        if ((mustHaveRatio >= 0.70 || (effectiveRatio >= 0.80 && strongMustHaves >= 4)) && "APPLY".equalsIgnoreCase(match.getRecommendation())) {
            if (freshness == JobFreshness.STALE) {
                // Stale jobs downranked to Medium Priority
                result.setPriorityCategory(PriorityCategory.MEDIUM_PRIORITY);
                result.setPriorityScore(Math.min(75, Math.max(55, baseScore - 15)));
            } else {
                result.setPriorityCategory(PriorityCategory.HIGH_PRIORITY);
                // Boost for fresh postings (NEW)
                int freshBonus = freshness == JobFreshness.NEW ? 5 : 0;
                result.setPriorityScore(Math.min(98, Math.max(82, baseScore + freshBonus)));
            }
            return;
        }

        // Rule 4: Medium Priority conditions
        if (mustHaveRatio >= 0.50 || "APPLY_AFTER_TAILORING".equalsIgnoreCase(match.getRecommendation())) {
            result.setPriorityCategory(PriorityCategory.MEDIUM_PRIORITY);
            result.setPriorityScore(Math.min(79, Math.max(50, baseScore)));
            return;
        }

        // Rule 5: Low Priority / Not Recommended default
        if (baseScore < 35 || mustHaveRatio < 0.3) {
            result.setPriorityCategory(PriorityCategory.NOT_RECOMMENDED);
            result.setPriorityScore(Math.min(35, Math.max(10, baseScore)));
        } else {
            result.setPriorityCategory(PriorityCategory.LOW_PRIORITY);
            result.setPriorityScore(Math.min(49, Math.max(25, baseScore)));
        }
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
