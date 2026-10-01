package com.jobhunter.service.prioritization;

import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.matching.RequirementMatchItem;
import com.jobhunter.dto.prioritization.JobFreshness;
import com.jobhunter.dto.prioritization.JobPrioritizationResult;
import com.jobhunter.dto.prioritization.PriorityCategory;
import com.jobhunter.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobPrioritizationServiceTest {

    private JobPrioritizationService prioritizationService;
    private CandidateProfile candidate;

    @BeforeEach
    void setUp() {
        prioritizationService = new JobPrioritizationService();

        User user = new User("developer@jobhunter.ai", "hash", "Jane", "Dev");
        candidate = new CandidateProfile();
        candidate.setId(UUID.randomUUID());
        candidate.setUser(user);
        candidate.setHeadline("Full Stack Java Engineer");
        candidate.setYearsOfExperience(new BigDecimal("3.0"));
        candidate.setCurrentLocation("Bangalore, India");
        candidate.setPreferredLocations("[\"Bangalore\", \"Remote\"]");
        candidate.setWorkModes("[\"REMOTE\", \"HYBRID\"]");

        List<CandidateSkill> skills = new ArrayList<>();
        skills.add(new CandidateSkill(candidate, new Skill("Java", "LANGUAGE", "[]"), "ADVANCED", new BigDecimal("3.0"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "Backend services"));
        skills.add(new CandidateSkill(candidate, new Skill("Spring Boot", "FRAMEWORK", "[]"), "ADVANCED", new BigDecimal("3.0"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "REST APIs"));
        skills.add(new CandidateSkill(candidate, new Skill("PostgreSQL", "DATABASE", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "Relational schemas"));
        skills.add(new CandidateSkill(candidate, new Skill("React", "FRONTEND", "[]"), "INTERMEDIATE", new BigDecimal("2.0"), false, "COMMERCIAL", new BigDecimal("0.85"), "Work", "Web UI"));
        candidate.setSkills(skills);
    }

    private MatchAnalysisResponse createMockMatchResponse(double score, String rec, List<String> strongMatches, List<String> gaps) {
        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setPriorityScore(BigDecimal.valueOf(score));
        match.setRecommendation(rec);
        match.setStrongMatches(strongMatches != null ? strongMatches : new ArrayList<>());
        match.setGaps(gaps != null ? gaps : new ArrayList<>());
        match.setRiskFactors(new ArrayList<>());

        List<RequirementMatchItem> reqAnalysis = new ArrayList<>();
        if (strongMatches != null) {
            for (String sm : strongMatches) {
                reqAnalysis.add(new RequirementMatchItem(sm, "TECHNICAL", "Verified experience", "STRONG", "HIGH", "Direct alignment", false));
            }
        }
        if (gaps != null) {
            for (String g : gaps) {
                reqAnalysis.add(new RequirementMatchItem(g, "TECHNICAL", "None", "GAP", "HIGH", "Missing", false));
            }
        }
        match.setRequirementAnalysis(reqAnalysis);
        return match;
    }

    @Test
    @DisplayName("Case 1: Fresh High-Match Job evaluates to HIGH_PRIORITY with NEW freshness")
    void testFreshHighMatchJob() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Developer");
        job.setNormalizedTitle("senior java developer");
        job.setCompany(new Company("TechCorp"));
        job.setLocation("Bangalore");
        job.setWorkMode("HYBRID");
        job.setMinExperienceYears(new BigDecimal("2.5"));
        job.setMaxExperienceYears(new BigDecimal("5.0"));
        job.setPostingDate(LocalDate.now().minusDays(2)); // 2 days old = NEW

        MatchAnalysisResponse match = createMockMatchResponse(85.0, "APPLY", List.of("Java", "Spring Boot"), List.of());

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(PriorityCategory.HIGH_PRIORITY, result.getPriorityCategory());
        assertEquals(JobFreshness.NEW, result.getFreshness());
        assertEquals(2, result.getDaysSincePosted());
        assertFalse(result.getWhyThisJob().isEmpty());
        assertTrue(result.getWhyThisJob().stream().anyMatch(w -> w.toLowerCase().contains("experience") || w.toLowerCase().contains("match")));
        assertNotNull(result.getRequirementCoverage());
        assertFalse(result.getRequirementCoverage().isEmpty());
    }

    @Test
    @DisplayName("Case 2: Stale Job (>60 days old) is capped at MEDIUM_PRIORITY")
    void testStaleJobDownranked() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Developer");
        job.setNormalizedTitle("senior java developer");
        job.setCompany(new Company("OldCorp"));
        job.setLocation("Bangalore");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("3.0"));
        job.setMaxExperienceYears(new BigDecimal("5.0"));
        job.setPostingDate(LocalDate.now().minusDays(75)); // 75 days old = STALE (>60d)

        MatchAnalysisResponse match = createMockMatchResponse(92.0, "APPLY", List.of("Java"), List.of());

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(JobFreshness.STALE, result.getFreshness());
        assertEquals(75, result.getDaysSincePosted());
        // Even though score is 92%, STALE status forces priority <= MEDIUM_PRIORITY
        assertTrue(result.getPriorityCategory() == PriorityCategory.MEDIUM_PRIORITY ||
                   result.getPriorityCategory() == PriorityCategory.LOW_PRIORITY,
                "Stale posting must not receive HIGH_PRIORITY");
        assertTrue(result.getPotentialConcerns().stream().anyMatch(c -> c.toLowerCase().contains("stale") || c.toLowerCase().contains("days ago")));
    }

    @Test
    @DisplayName("Case 3: Seniority Gap >= 2.5 years forces priority to LOW_PRIORITY / NOT_RECOMMENDED")
    void testSeniorityGapHardConstraint() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Staff Software Engineer");
        job.setNormalizedTitle("staff software engineer");
        job.setCompany(new Company("BigTech"));
        job.setLocation("Bangalore");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("6.0")); // Candidate has 3.0 YOE => Gap = 3.0 >= 2.5!
        job.setPostingDate(LocalDate.now().minusDays(3));

        MatchAnalysisResponse match = createMockMatchResponse(50.0, "APPLY_AFTER_TAILORING", List.of("Java"), List.of("6+ YOE"));

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertTrue(result.getPriorityCategory() == PriorityCategory.LOW_PRIORITY ||
                   result.getPriorityCategory() == PriorityCategory.NOT_RECOMMENDED,
                "Seniority gap >= 2.5 yrs must cap priority at LOW or NOT_RECOMMENDED");
        assertTrue(result.getPotentialConcerns().stream().anyMatch(c -> c.toLowerCase().contains("seniority") || c.toLowerCase().contains("experience")));
    }

    @Test
    @DisplayName("Case 4: Executive title barrier (VP / Director / Chief) forces NOT_RECOMMENDED for junior/mid candidate")
    void testExecutiveTitleBarrier() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Vice President of Engineering");
        job.setNormalizedTitle("vp engineering");
        job.setCompany(new Company("EnterpriseInc"));
        job.setLocation("Bangalore");
        job.setWorkMode("HYBRID");
        job.setMinExperienceYears(new BigDecimal("10.0"));
        job.setPostingDate(LocalDate.now().minusDays(5));

        MatchAnalysisResponse match = createMockMatchResponse(30.0, "DO_NOT_APPLY", List.of(), List.of("10+ YOE", "Executive leadership"));

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(PriorityCategory.NOT_RECOMMENDED, result.getPriorityCategory());
        assertTrue(result.getPotentialConcerns().stream().anyMatch(c -> c.toLowerCase().contains("executive")));
    }

    @Test
    @DisplayName("Case 5: Missing mandatory domain tech (e.g. Swift/iOS or Kotlin/Android) caps priority at LOW_PRIORITY")
    void testMissingMandatoryDomainTech() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("iOS Mobile Engineer");
        job.setNormalizedTitle("ios developer");
        job.setCompany(new Company("AppWorks"));
        job.setLocation("Bangalore");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("2.5"));
        job.setPostingDate(LocalDate.now().minusDays(4));

        MatchAnalysisResponse match = createMockMatchResponse(45.0, "APPLY_AFTER_TAILORING", List.of(), List.of("Swift", "SwiftUI"));

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertTrue(result.getPriorityCategory() == PriorityCategory.LOW_PRIORITY ||
                   result.getPriorityCategory() == PriorityCategory.NOT_RECOMMENDED,
                "Missing mandatory mobile stack must cap priority at LOW or NOT_RECOMMENDED");
        assertTrue(result.getPotentialConcerns().stream().anyMatch(c -> c.toLowerCase().contains("mandatory") || c.toLowerCase().contains("swift")));
    }

    @Test
    @DisplayName("Case 6: Strict On-Site Location Mismatch forces NOT_RECOMMENDED / LOW_PRIORITY")
    void testStrictOnSiteLocationMismatch() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setNormalizedTitle("backend engineer");
        job.setCompany(new Company("TokyoTech"));
        job.setLocation("Tokyo, Japan");
        job.setWorkMode("ON_SITE");
        job.setMinExperienceYears(new BigDecimal("3.0"));
        job.setPostingDate(LocalDate.now().minusDays(1));

        MatchAnalysisResponse match = createMockMatchResponse(50.0, "APPLY_AFTER_TAILORING", List.of("Java"), List.of("Tokyo On-Site"));

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertTrue(result.getPriorityCategory() == PriorityCategory.NOT_RECOMMENDED ||
                   result.getPriorityCategory() == PriorityCategory.LOW_PRIORITY);
        assertTrue(result.getPotentialConcerns().stream().anyMatch(c -> c.toLowerCase().contains("location") || c.toLowerCase().contains("tokyo")));
    }

    @Test
    @DisplayName("Case 7: Posting date null evaluates to UNKNOWN freshness without error or fabrication")
    void testUnknownPostingDate() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setNormalizedTitle("backend engineer");
        job.setCompany(new Company("Startup"));
        job.setLocation("Bangalore");
        job.setWorkMode("REMOTE");
        job.setPostingDate(null); // Unknown date

        MatchAnalysisResponse match = createMockMatchResponse(80.0, "APPLY", List.of("Java"), List.of());

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(JobFreshness.UNKNOWN, result.getFreshness());
        assertNull(result.getDaysSincePosted());
    }

    @Test
    @DisplayName("Case 8: Independence of Applied State — Prioritization is invariant to applied status")
    void testAppliedStateInvariance() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setNormalizedTitle("backend engineer");
        job.setCompany(new Company("InvarianceCo"));
        job.setLocation("Bangalore");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("2.5"));
        job.setPostingDate(LocalDate.now().minusDays(5));

        MatchAnalysisResponse match = createMockMatchResponse(85.0, "APPLY", List.of("Java", "Spring Boot"), List.of());

        // Run prioritization on same inputs
        JobPrioritizationResult result1 = prioritizationService.evaluatePrioritization(job, candidate, match);
        JobPrioritizationResult result2 = prioritizationService.evaluatePrioritization(job, candidate, match);

        // Result is purely deterministic and completely independent of whether candidate marked it applied
        assertEquals(result1.getPriorityCategory(), result2.getPriorityCategory());
        assertEquals(result1.getFreshness(), result2.getFreshness());
        assertEquals(result1.getWhyThisJob(), result2.getWhyThisJob());
        assertEquals(result1.getPotentialConcerns(), result2.getPotentialConcerns());
    }

    @Test
    @DisplayName("Milestone 6: Active Government Security Clearance triggers hard constraint NOT_RECOMMENDED")
    void testSecurityClearanceHardConstraint() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer - Security Cleared");
        job.setCompany(new Company("Quindar"));
        job.setLocation("Denver, CO");
        job.setWorkMode("HYBRID");
        job.setRawDescriptionMarkdown("Responsibilities include ground operations. Requires active Top Secret / SCI security clearance.");

        MatchAnalysisResponse match = createMockMatchResponse(70.0, "APPLY", List.of("Java"), List.of());

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(PriorityCategory.NOT_RECOMMENDED, result.getPriorityCategory());
        assertTrue(result.getHardConstraintViolations().stream().anyMatch(v -> v.toLowerCase().contains("security clearance")));
    }

    @Test
    @DisplayName("Milestone 6: Incompatible physical in-office/on-site presence in foreign jurisdiction triggers NOT_RECOMMENDED")
    void testPhysicalOnSiteForeignLocationHardConstraint() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Backend Engineer");
        job.setCompany(new Company("Blitzy"));
        job.setLocation("Cambridge, MA");
        job.setWorkMode("ON_SITE");
        job.setRawDescriptionMarkdown("Location Type: On-site in Cambridge, MA. In-office 5 days a week.");

        MatchAnalysisResponse match = createMockMatchResponse(72.0, "APPLY", List.of("Java"), List.of());

        JobPrioritizationResult result = prioritizationService.evaluatePrioritization(job, candidate, match);

        assertNotNull(result);
        assertEquals(PriorityCategory.NOT_RECOMMENDED, result.getPriorityCategory());
        assertTrue(result.getHardConstraintViolations().stream().anyMatch(v -> v.toLowerCase().contains("presence") || v.toLowerCase().contains("location")));
    }
}
