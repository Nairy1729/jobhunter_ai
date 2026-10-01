package com.jobhunter.service.tailoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.tailoring.TailoredResumeResponse;
import com.jobhunter.dto.tailoring.TailoringDiffDto;
import com.jobhunter.dto.tailoring.TailoringPlanDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.fact.CandidateFactStoreService;
import com.jobhunter.service.matching.GroundingVerificationGate;
import com.jobhunter.service.matching.SemanticMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ResumeTailoringServiceTest {

    private JobRepository jobRepository;
    private CandidateProfileRepository profileRepository;
    private TailoredResumeRepository tailoredResumeRepository;
    private ResumeRepository resumeRepository;
    private CandidateExperienceRepository experienceRepository;
    private CandidateProjectRepository projectRepository;
    private CandidateSkillRepository candidateSkillRepository;
    private CandidateFactRepository candidateFactRepository;
    private ResumeTailoringAuditRepository auditRepository;
    private SemanticMatchingService semanticMatchingService;
    private GroundingVerificationGate groundingGate;
    private PdfGenerationService pdfGenerationService;
    private CandidateFactStoreService factStoreService;
    private ResumeClaimAuditor claimAuditor;
    private ObjectMapper objectMapper;

    private ResumeTailoringService tailoringService;

    private User candidateUser;
    private CandidateProfile candidateProfile;
    private Job javaJob;
    private Job swiftJob;
    private Resume masterResume;

    @BeforeEach
    void setUp() throws IOException {
        jobRepository = Mockito.mock(JobRepository.class);
        profileRepository = Mockito.mock(CandidateProfileRepository.class);
        tailoredResumeRepository = Mockito.mock(TailoredResumeRepository.class);
        resumeRepository = Mockito.mock(ResumeRepository.class);
        experienceRepository = Mockito.mock(CandidateExperienceRepository.class);
        projectRepository = Mockito.mock(CandidateProjectRepository.class);
        candidateSkillRepository = Mockito.mock(CandidateSkillRepository.class);
        candidateFactRepository = Mockito.mock(CandidateFactRepository.class);
        auditRepository = Mockito.mock(ResumeTailoringAuditRepository.class);
        semanticMatchingService = Mockito.mock(SemanticMatchingService.class);
        groundingGate = new GroundingVerificationGate();
        objectMapper = new ObjectMapper();
        pdfGenerationService = new PdfGenerationService(objectMapper);

        when(candidateFactRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        factStoreService = new CandidateFactStoreService(
                candidateFactRepository,
                experienceRepository,
                projectRepository,
                candidateSkillRepository,
                resumeRepository,
                objectMapper
        );

        claimAuditor = new ResumeClaimAuditor(factStoreService);

        Path tempUpload = Files.createTempDirectory("tailor_upload_test_");

        tailoringService = new ResumeTailoringService(
                jobRepository,
                profileRepository,
                tailoredResumeRepository,
                resumeRepository,
                experienceRepository,
                projectRepository,
                candidateSkillRepository,
                semanticMatchingService,
                groundingGate,
                pdfGenerationService,
                factStoreService,
                claimAuditor,
                auditRepository,
                objectMapper,
                tempUpload.toAbsolutePath().toString()
        );

        candidateUser = new User("candidate@jobhunter.ai", "pass", "Alex", "Dev");
        candidateUser.setId(UUID.randomUUID());

        candidateProfile = new CandidateProfile();
        candidateProfile.setId(UUID.randomUUID());
        candidateProfile.setUser(candidateUser);
        candidateProfile.setHeadline("Software Engineer / Backend Engineer");
        candidateProfile.setSummary("Software Engineer with 2.5 years of experience in Java, Spring Boot, and PostgreSQL.");
        candidateProfile.setYearsOfExperience(new BigDecimal("2.5"));
        candidateProfile.setCurrentLocation("Bengaluru, India");

        Skill javaSkill = new Skill("Java", "LANGUAGE", "[]");
        Skill springSkill = new Skill("Spring Boot", "FRAMEWORK", "[]");
        Skill pgSkill = new Skill("PostgreSQL", "DATABASE", "[]");
        Skill dockerSkill = new Skill("Docker", "DEVOPS", "[]");

        CandidateSkill cs1 = new CandidateSkill(candidateProfile, javaSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs1.setExperienceType("COMMERCIAL");
        CandidateSkill cs2 = new CandidateSkill(candidateProfile, springSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs2.setExperienceType("COMMERCIAL");
        CandidateSkill cs3 = new CandidateSkill(candidateProfile, pgSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs3.setExperienceType("COMMERCIAL");
        CandidateSkill cs4 = new CandidateSkill(candidateProfile, dockerSkill, "INTERMEDIATE", new BigDecimal("1.0"), false, "Commercial");
        cs4.setExperienceType("COMMERCIAL");
        candidateProfile.setSkills(new ArrayList<>(List.of(cs1, cs2, cs3, cs4)));

        CandidateExperience exp = new CandidateExperience(
                candidateProfile,
                "FinTech SaaS Solutions",
                "Software Engineer",
                "Jan 2024 - Present",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Engineered high-reliability transaction reconciliation REST APIs\"]",
                "[\"Reduced batch report generation latency from 14s to 1.8s through query plan optimization\"]",
                "FinTech",
                "Commercial employment"
        );
        candidateProfile.setExperiences(new ArrayList<>(List.of(exp)));

        CandidateProject proj = new CandidateProject(
                candidateProfile,
                "Mentor-Mentee Collaboration Platform",
                "Mentoring platform",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "Microservice architecture",
                "[\"Designed normalized database schema\"]",
                "[\"Sustained 100+ concurrent simulated sessions without message loss\"]",
                "GitHub",
                "https://github.com/candidate-dev/mentor-mentee-platform"
        );
        candidateProfile.setProjects(new ArrayList<>(List.of(proj)));

        Company fintech = new Company("FinTech Solutions");
        javaJob = new Job();
        javaJob.setId(UUID.randomUUID());
        javaJob.setCompany(fintech);
        javaJob.setTitle("Software Engineer (Java / Spring Boot)");
        javaJob.setNormalizedTitle("Software Engineer");
        javaJob.setRawDescriptionMarkdown("Looking for a Java 17 and Spring Boot engineer with PostgreSQL, Kafka, and AWS cloud experience.");

        Company apple = new Company("Apple");
        swiftJob = new Job();
        swiftJob.setId(UUID.randomUUID());
        swiftJob.setCompany(apple);
        swiftJob.setTitle("Senior iOS Engineer (Swift / Objective-C)");
        swiftJob.setNormalizedTitle("Senior iOS Engineer");
        swiftJob.setRawDescriptionMarkdown("Deep expertise in Swift, Objective-C, and Cocoa Touch required. 5+ years experience.");

        masterResume = new Resume();
        masterResume.setId(UUID.randomUUID());
        masterResume.setCandidateProfile(candidateProfile);
        masterResume.setTitle("Master_Resume.pdf");
        masterResume.setMaster(true);
        masterResume.setRawExtractedText("Alex Dev - Master Resume Text");

        when(profileRepository.findByUserId(candidateUser.getId())).thenReturn(Optional.of(candidateProfile));
        when(experienceRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(exp));
        when(projectRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(proj));
        when(candidateSkillRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(cs1, cs2, cs3, cs4));
        when(resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(candidateProfile.getId())).thenReturn(Optional.of(masterResume));
    }

    @Test
    @DisplayName("Zero-Hallucination Policy: Unverified JD keywords must be rejected and logged in rejectedKeywords")
    void testGenerateTailoringPlanWithZeroHallucination() {
        when(jobRepository.findById(javaJob.getId())).thenReturn(Optional.of(javaJob));

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setStrongMatches(List.of("Java", "Spring Boot", "PostgreSQL"));
        when(semanticMatchingService.analyzeAndMatch(javaJob.getId(), candidateUser.getId())).thenReturn(match);

        TailoringPlanDto plan = tailoringService.generateTailoringPlan(javaJob.getId(), candidateUser.getId());

        assertNotNull(plan);
        // Candidate has Java, Spring Boot, PostgreSQL
        assertTrue(plan.getSkillsToEmphasize().contains("Java"));
        assertTrue(plan.getSkillsToEmphasize().contains("Spring Boot"));
        assertTrue(plan.getSkillsToEmphasize().contains("PostgreSQL"));

        // JD also mentioned Kafka and AWS - candidate has NO evidence for them!
        // The Grounding Gate MUST REJECT Kafka and AWS from tailored resume!
        assertTrue(plan.getRejectedKeywords().stream().anyMatch(r -> r.getKeyword().equalsIgnoreCase("Kafka")),
                "Kafka must be rejected because candidate has no verified evidence");
        assertTrue(plan.getRejectedKeywords().stream().anyMatch(r -> r.getKeyword().equalsIgnoreCase("AWS")),
                "AWS must be rejected because candidate has no verified evidence");

        // The rejection reason must be transparent and documented
        String kafkaReason = plan.getRejectedKeywords().stream()
                .filter(r -> r.getKeyword().equalsIgnoreCase("Kafka"))
                .findFirst().get().getReason();
        assertTrue(kafkaReason.contains("no verified") && kafkaReason.contains("Kafka"));
    }

    @Test
    @DisplayName("Should propose grounded bullet sharpening strictly derived from candidate real commercial achievements")
    void testGroundedBulletSharpening() {
        when(jobRepository.findById(javaJob.getId())).thenReturn(Optional.of(javaJob));

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setStrongMatches(List.of("Java", "Spring Boot", "PostgreSQL"));
        when(semanticMatchingService.analyzeAndMatch(javaJob.getId(), candidateUser.getId())).thenReturn(match);

        TailoringPlanDto plan = tailoringService.generateTailoringPlan(javaJob.getId(), candidateUser.getId());

        assertFalse(plan.getBulletSharpeningProposals().isEmpty());

        // Check query optimization bullet
        var queryBullet = plan.getBulletSharpeningProposals().stream()
                .filter(b -> b.getOriginalBullet().contains("latency") || b.getOriginalBullet().contains("reconciliation"))
                .findFirst();

        assertTrue(queryBullet.isPresent());
        assertEquals("VERIFIED_GROUNDED", queryBullet.get().getGroundingStatus());
        assertNotNull(queryBullet.get().getProposedBullet());
    }

    @Test
    @DisplayName("Should execute full tailoring pipeline, pass Stage 1 and Stage 2 claim audits, and enter READY_FOR_DOWNLOAD")
    void testTailorResumePipelineGeneratesCompletePackage() {
        when(jobRepository.findById(javaJob.getId())).thenReturn(Optional.of(javaJob));

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setStrongMatches(List.of("Java", "Spring Boot", "PostgreSQL"));
        when(semanticMatchingService.analyzeAndMatch(javaJob.getId(), candidateUser.getId())).thenReturn(match);

        when(tailoredResumeRepository.save(any(TailoredResume.class))).thenAnswer(invocation -> {
            TailoredResume r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TailoredResumeResponse response = tailoringService.tailorResume(javaJob.getId(), candidateUser.getId());

        assertNotNull(response);
        assertEquals(1, response.getVersionNumber());
        assertEquals("READY_FOR_DOWNLOAD", response.getStatus());
        assertTrue(response.isPdfAvailable(), "PDF must be available");
        assertNotNull(response.getTailoredMarkdown());
        assertNotNull(response.getLatexSource());
        assertTrue(response.getAtsScoreEstimate().doubleValue() >= 70.0);
        assertTrue(response.getValidationReport().isPassed());

        // Verify master resume remains immutable
        assertEquals("Alex Dev - Master Resume Text", masterResume.getRawExtractedText());
        assertTrue(masterResume.isMaster());
    }

    @Test
    @DisplayName("Should generate accurate Before/After diff comparing Master and Tailored resumes")
    void testGetTailoringDiffTracksChanges() {
        UUID tailoredId = UUID.randomUUID();
        TailoredResume tr = new TailoredResume();
        tr.setId(tailoredId);
        tr.setCandidateProfile(candidateProfile);
        tr.setMasterResume(masterResume);
        tr.setJob(javaJob);
        tr.setTailoredMarkdown("# Alex Dev\nTailored Content");

        TailoringPlanDto plan = new TailoringPlanDto();
        plan.getSkillsToEmphasize().add("Java");
        plan.getSkillsToDeemphasize().add("React");
        plan.getRejectedKeywords().add(new com.jobhunter.dto.tailoring.RejectedKeywordItem("Kafka", "No evidence"));
        try {
            tr.setTailoringPlan(objectMapper.writeValueAsString(plan));
        } catch (Exception ignored) {}

        when(tailoredResumeRepository.findByIdAndUserId(tailoredId, candidateUser.getId())).thenReturn(Optional.of(tr));

        TailoringDiffDto diff = tailoringService.getTailoringDiff(tailoredId, candidateUser.getId());

        assertNotNull(diff);
        assertEquals("Alex Dev - Master Resume Text", diff.getMasterResumeContent());
        assertEquals("# Alex Dev\nTailored Content", diff.getTailoredResumeContent());
        assertTrue(diff.getAddedEmphasis().stream().anyMatch(e -> e.contains("Java")));
        assertTrue(diff.getRemovedOrDeemphasized().stream().anyMatch(e -> e.contains("React")));
        assertTrue(diff.getRejectedKeywords().stream().anyMatch(r -> r.getKeyword().equals("Kafka")));
    }

    @Test
    @DisplayName("Candidate Data Isolation: Should deny access when requesting tailored resume of another user")
    void testSecurityCandidateIsolation() {
        UUID tailoredId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        when(tailoredResumeRepository.findByIdAndUserId(tailoredId, otherUserId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            tailoringService.getTailoredResume(tailoredId, otherUserId);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tailoringService.getTailoringDiff(tailoredId, otherUserId);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            tailoringService.getPdfFile(tailoredId, otherUserId);
        });
    }
}
