package com.jobhunter.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.matching.UsefulnessDecision;
import com.jobhunter.dto.profile.ProfileReadinessReport;
import com.jobhunter.dto.profile.ProfileReadinessState;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.discovery.HardEligibilityFilterService;
import com.jobhunter.service.discovery.dto.HardEligibilityResult;
import com.jobhunter.service.matching.UsefulnessGateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileReadinessServiceTest {

    @Mock
    private CandidateProfileRepository profileRepository;

    @Mock
    private CandidateExperienceRepository experienceRepository;

    @Mock
    private CandidateSkillRepository candidateSkillRepository;

    @Mock
    private ResumeRepository resumeRepository;

    private ObjectMapper objectMapper;
    private ProfileReadinessService readinessService;
    private HardEligibilityFilterService hardEligibilityFilter;
    private UsefulnessGateService usefulnessGate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        readinessService = new ProfileReadinessService(
                profileRepository,
                experienceRepository,
                candidateSkillRepository,
                resumeRepository,
                objectMapper
        );
        hardEligibilityFilter = new HardEligibilityFilterService(objectMapper);
        usefulnessGate = new UsefulnessGateService(hardEligibilityFilter);
    }

    @Test
    @DisplayName("Test 1: Incomplete profile missing master resume blocks discovery")
    void testIncompleteProfile_MissingResume_BlocksDiscovery() {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Software Engineer");
        profile.setTargetRoles("[\"Backend Engineer\"]");
        profile.setPreferredLocations("[\"Bangalore\"]");
        profile.setWorkModes("[\"REMOTE\"]");
        profile.setYearsOfExperience(new BigDecimal("2.5"));

        when(resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId()))
                .thenReturn(Optional.empty());

        ProfileReadinessReport report = readinessService.evaluateProfile(profile);

        assertEquals(ProfileReadinessState.PROFILE_NOT_READY, report.getState());
        assertFalse(report.isCanDiscover());
        assertTrue(report.getMissingItems().contains("Resume"));
        assertEquals("YOUR PROFILE ISN'T READY", report.getHeadline());
    }

    @Test
    @DisplayName("Test 2: Complete profile with verified facts unlocks discovery")
    void testCompleteProfile_UnlocksDiscovery() {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Software Engineer / Backend Developer");
        profile.setSummary("2.5 years of Java Spring Boot experience. B.Tech in Computer Science.");
        profile.setTargetRoles("[\"Backend Engineer\", \"Java Developer\"]");
        profile.setPreferredLocations("[\"Bangalore\", \"Remote\"]");
        profile.setWorkModes("[\"REMOTE\", \"HYBRID\"]");
        profile.setYearsOfExperience(new BigDecimal("2.5"));
        profile.setCurrentLocation("Bangalore, India");

        Resume masterResume = new Resume();
        masterResume.setRawExtractedText("ALEX ENGINEER\nSoftware Engineer with 2.5 years experience.\nEDUCATION: Bachelor of Technology in CS\nSKILLS: Java, Spring Boot, PostgreSQL");
        when(resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId()))
                .thenReturn(Optional.of(masterResume));

        CandidateExperience exp = new CandidateExperience();
        exp.setRole("Software Engineer");
        exp.setCompany("FinTech SaaS");
        when(experienceRepository.findByCandidateProfileId(profile.getId()))
                .thenReturn(List.of(exp));

        CandidateSkill s1 = new CandidateSkill();
        CandidateSkill s2 = new CandidateSkill();
        CandidateSkill s3 = new CandidateSkill();
        when(candidateSkillRepository.findByCandidateProfileId(profile.getId()))
                .thenReturn(List.of(s1, s2, s3));

        ProfileReadinessReport report = readinessService.evaluateProfile(profile);

        assertEquals(ProfileReadinessState.PROFILE_READY, report.getState());
        assertTrue(report.isCanDiscover());
        assertTrue(report.getMissingItems().isEmpty());
        assertTrue(report.getScore() >= 80);
        assertEquals("PROFILE READY", report.getHeadline());
    }

    @Test
    @DisplayName("Test 3: Hard Filter rejects severe seniority mismatch (8+ yrs required vs 2.5 yrs)")
    void testHardFilter_SevereSeniorityMismatch_Rejected() {
        CandidateProfile profile = new CandidateProfile();
        profile.setYearsOfExperience(new BigDecimal("2.5"));
        profile.setTargetRoles("[\"Software Engineer\"]");

        Job seniorJob = new Job();
        seniorJob.setTitle("Principal Architect / Staff Software Engineer");
        seniorJob.setMinExperienceYears(new BigDecimal("8.0"));
        seniorJob.setRawDescriptionMarkdown("Requires 8+ years of distributed systems experience.");

        HardEligibilityResult result = hardEligibilityFilter.evaluate(seniorJob, profile);

        assertFalse(result.isEligible());
        assertEquals("SENIORITY_OVERFLOW", result.getMismatchCategory());
    }

    @Test
    @DisplayName("Test 4: Hard Filter rejects domain mismatch (Registered Nurse vs Software Engineer)")
    void testHardFilter_DomainMismatch_Rejected() {
        CandidateProfile profile = new CandidateProfile();
        profile.setYearsOfExperience(new BigDecimal("2.5"));
        profile.setTargetRoles("[\"Software Engineer\", \"Backend Developer\"]");

        Job nurseJob = new Job();
        nurseJob.setTitle("Registered Nurse - Critical Care ICU");
        nurseJob.setRawDescriptionMarkdown("Full-time ICU clinical care.");

        HardEligibilityResult result = hardEligibilityFilter.evaluate(nurseJob, profile);

        assertFalse(result.isEligible());
        assertEquals("ROLE_DOMAIN_MISMATCH", result.getMismatchCategory());
    }

    @Test
    @DisplayName("Test 5: Hard Filter permits legitimate engineering role")
    void testHardFilter_LegitimateRole_Accepted() {
        CandidateProfile profile = new CandidateProfile();
        profile.setYearsOfExperience(new BigDecimal("2.5"));
        profile.setTargetRoles("[\"Backend Engineer\"]");
        profile.setCurrentLocation("Bangalore, India");
        profile.setPreferredLocations("[\"Bangalore\", \"Remote\"]");
        profile.setWorkModes("[\"HYBRID\", \"REMOTE\"]");

        Job relevantJob = new Job();
        relevantJob.setTitle("Software Engineer (Java / Spring Boot)");
        relevantJob.setMinExperienceYears(new BigDecimal("2.0"));
        relevantJob.setLocation("Bengaluru, India");
        relevantJob.setWorkMode("HYBRID");
        relevantJob.setRawDescriptionMarkdown("2+ years of Java and Spring Boot experience.");

        HardEligibilityResult result = hardEligibilityFilter.evaluate(relevantJob, profile);

        assertTrue(result.isEligible());
        assertEquals("NONE", result.getMismatchCategory());
    }

    @Test
    @DisplayName("Test 6: Usefulness Gate classifies strong alignment as USEFUL / HIGH_RELEVANCE")
    void testUsefulnessGate_HighRelevance_SurfacesToFeed() {
        CandidateProfile profile = new CandidateProfile();
        profile.setYearsOfExperience(new BigDecimal("2.5"));

        Job job = new Job();
        job.setTitle("Backend Engineer (Java / Spring)");

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setPriorityScore(new BigDecimal("82.5"));
        match.setRecommendation("APPLY");
        match.setStrongMatches(List.of("Java 17", "Spring Boot microservices", "PostgreSQL"));
        match.setSupportingEvidence(List.of("Commercial track record at FinTech SaaS"));

        UsefulnessDecision decision = usefulnessGate.evaluateUsefulness(job, profile, match);

        assertTrue(decision.isUseful());
        assertEquals("USEFUL", decision.getUsefulnessStatus());
        assertEquals("HIGH_RELEVANCE", decision.getMatchCategory());
    }

    @Test
    @DisplayName("Test 7: Usefulness Gate drops DO_NOT_APPLY and unaligned jobs (NOT_USEFUL)")
    void testUsefulnessGate_DoNotApply_FilteredOut() {
        CandidateProfile profile = new CandidateProfile();
        profile.setYearsOfExperience(new BigDecimal("2.5"));

        Job job = new Job();
        job.setTitle("Senior iOS Swift Developer");

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setPriorityScore(new BigDecimal("22.0"));
        match.setRecommendation("DO_NOT_APPLY");
        match.setStrongMatches(Collections.emptyList());
        match.setPartialMatches(Collections.emptyList());
        match.setGaps(List.of("Swift", "UIKit", "Cocoa"));

        UsefulnessDecision decision = usefulnessGate.evaluateUsefulness(job, profile, match);

        assertFalse(decision.isUseful());
        assertEquals("NOT_USEFUL", decision.getUsefulnessStatus());
        assertEquals("NOT_USEFUL", decision.getMatchCategory());
    }
}
