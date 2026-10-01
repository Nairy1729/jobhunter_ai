package com.jobhunter.service.tailoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.tailoring.AuditedClaim;
import com.jobhunter.dto.tailoring.TailoredResumeResponse;
import com.jobhunter.dto.tailoring.TailoringAuditReportDto;
import com.jobhunter.dto.tailoring.TailoringPlanDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.model.fact.ClaimType;
import com.jobhunter.model.fact.SkillEvidenceType;
import com.jobhunter.model.tailoring.TailoredResumeDocument;
import com.jobhunter.model.tailoring.ValidationStatus;
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

/**
 * OfferPilot-Style Resume Tailoring Behavioral & Integrity Test Suite.
 * Validates the core principle: Maximum Linguistic Freedom + Zero Factual Fabrication.
 * Covers 7 Required Scenarios (Test A through Test G).
 */
class OfferPilotTailoringTest {

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
    private Job targetJob;
    private Resume masterResume;
    private CandidateExperience razorpayExp;
    private CandidateProject mentorProject;

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

        Path tempUpload = Files.createTempDirectory("offerpilot_test_upload_");

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

        candidateUser = new User("candidate@offerpilot.ai", "secret", "Jane", "Doe");
        candidateUser.setId(UUID.randomUUID());

        candidateProfile = new CandidateProfile();
        candidateProfile.setId(UUID.randomUUID());
        candidateProfile.setUser(candidateUser);
        candidateProfile.setHeadline("Software Engineer / Backend Developer");
        candidateProfile.setSummary("Software Engineer with 2.5 years of experience in Java, Spring Boot, and PostgreSQL.");
        candidateProfile.setYearsOfExperience(new BigDecimal("2.5"));
        candidateProfile.setCurrentLocation("Bengaluru, India");
        candidateProfile.setPhoneNumber("+91 98765 43210");
        candidateProfile.setRawProfileData("{\"education\": [{\"degree\": \"B.Tech in Electronics and Communication Engineering\", \"institution\": \"VIT Vellore\", \"dates\": \"2018 - 2022\"}]}");

        // Verified Skills: Java, Spring Boot, PostgreSQL
        Skill s1 = new Skill("Java", "LANGUAGE", "[]");
        Skill s2 = new Skill("Spring Boot", "FRAMEWORK", "[]");
        Skill s3 = new Skill("PostgreSQL", "DATABASE", "[]");

        CandidateSkill cs1 = new CandidateSkill(candidateProfile, s1, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs1.setExperienceType("COMMERCIAL");
        CandidateSkill cs2 = new CandidateSkill(candidateProfile, s2, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs2.setExperienceType("COMMERCIAL");
        CandidateSkill cs3 = new CandidateSkill(candidateProfile, s3, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        cs3.setExperienceType("COMMERCIAL");
        candidateProfile.setSkills(new ArrayList<>(List.of(cs1, cs2, cs3)));

        // Verified Experience at Razorpay with verified metric (30% latency reduction)
        razorpayExp = new CandidateExperience(
                candidateProfile,
                "Razorpay",
                "Software Engineer",
                "Jan 2023 - Present",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Worked on backend APIs.\", \"Built transaction processing service\"]",
                "[\"Reduced API latency by 30%\"]",
                "FinTech",
                "Commercial employment"
        );
        razorpayExp.setId(UUID.randomUUID());
        candidateProfile.setExperiences(new ArrayList<>(List.of(razorpayExp)));

        // Verified Technical Project
        mentorProject = new CandidateProject(
                candidateProfile,
                "Payment Reconciliation Engine",
                "High-reliability payment reconciliation platform",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "Microservice architecture",
                "[\"Designed normalized database schema\"]",
                "[\"Sustained 100+ concurrent simulated sessions without message loss\"]",
                "GitHub",
                "https://github.com/janedoe/reconciliation-engine"
        );
        mentorProject.setId(UUID.randomUUID());
        candidateProfile.setProjects(new ArrayList<>(List.of(mentorProject)));

        // Target Job: Senior Backend Engineer (Demands Java, Spring Boot, PostgreSQL, Kafka)
        Company targetCo = new Company("Razorpay Capital");
        targetJob = new Job();
        targetJob.setId(UUID.randomUUID());
        targetJob.setCompany(targetCo);
        targetJob.setTitle("Senior Backend Engineer (Java / Distributed Systems)");
        targetJob.setNormalizedTitle("Senior Backend Engineer");
        targetJob.setRawDescriptionMarkdown("We are looking for a Senior Backend Engineer proficient in Java, Spring Boot, PostgreSQL, and Kafka event streaming.");

        masterResume = new Resume();
        masterResume.setId(UUID.randomUUID());
        masterResume.setCandidateProfile(candidateProfile);
        masterResume.setTitle("Jane_Doe_Master_Resume.pdf");
        masterResume.setMaster(true);
        masterResume.setRawExtractedText("Jane Doe - Master Resume Content");

        when(profileRepository.findByUserId(candidateUser.getId())).thenReturn(Optional.of(candidateProfile));
        when(experienceRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(razorpayExp));
        when(projectRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(mentorProject));
        when(candidateSkillRepository.findByCandidateProfileId(candidateProfile.getId())).thenReturn(List.of(cs1, cs2, cs3));
        when(resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(candidateProfile.getId())).thenReturn(Optional.of(masterResume));
    }

    @Test
    @DisplayName("Test A: Legitimate aggressive tailoring (PASS) - Rewrites summary, reorders skills, sharpens bullets, passes audit")
    void testA_LegitimateAggressiveTailoring_Pass() {
        when(jobRepository.findById(targetJob.getId())).thenReturn(Optional.of(targetJob));

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setStrongMatches(List.of("Java", "Spring Boot", "PostgreSQL"));
        when(semanticMatchingService.analyzeAndMatch(targetJob.getId(), candidateUser.getId())).thenReturn(match);

        when(tailoredResumeRepository.save(any(TailoredResume.class))).thenAnswer(inv -> {
            TailoredResume r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TailoredResumeResponse response = tailoringService.tailorResume(targetJob.getId(), candidateUser.getId());

        assertNotNull(response);
        assertEquals("READY_FOR_DOWNLOAD", response.getStatus());
        assertTrue(response.isPdfAvailable());
        assertTrue(response.getValidationReport().isPassed());
        assertTrue(response.getValidationReport().getFailedChecks().isEmpty());

        // Check that summary was aggressively tailored for the target role
        assertNotNull(response.getTailoredMarkdown());
        assertTrue(response.getTailoredMarkdown().contains("Backend Software Engineer"));
        assertTrue(response.getTailoredMarkdown().contains("Java, Spring Boot, PostgreSQL") ||
                response.getTailoredMarkdown().contains("Java, Spring Boot, and PostgreSQL"));
        assertTrue(response.getTailoredMarkdown().contains("Razorpay"));

        // Check that B.Tech degree is truthfully preserved
        assertTrue(response.getTailoredMarkdown().contains("B.Tech in Electronics and Communication Engineering"));
    }

    @Test
    @DisplayName("Test B: Legitimate synthesis combining multiple facts (PASS) - Synthesizes verified skills & metrics into active engineering bullet")
    void testB_LegitimateSynthesisCombiningMultipleFacts_Pass() {
        // Candidate has verified Razorpay employment, verified Spring Boot/PostgreSQL skills, and verified 30% latency reduction.
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(candidateProfile.getId());
        doc.setJobId(targetJob.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@offerpilot.ai";
        doc.getSummary().text = "Backend Software Engineer specializing in Java, Spring Boot, and PostgreSQL.";

        TailoredResumeDocument.ExperienceItem exp = new TailoredResumeDocument.ExperienceItem();
        exp.company = "Razorpay";
        exp.role = "Software Engineer";
        exp.duration = "Jan 2023 - Present";

        // Multi-fact synthesis: Combines payment service + Spring Boot + PostgreSQL + verified 30% latency reduction
        String synthesizedBullet = "Architected high-throughput payment services in Spring Boot and PostgreSQL, optimizing database query execution to reduce latency by 30%.";
        TailoredResumeDocument.ExperienceBullet bullet = new TailoredResumeDocument.ExperienceBullet(
                synthesizedBullet, razorpayExp.getId(), List.of(razorpayExp.getId()), "DERIVED_FROM_SUPPORTED_FACTS"
        );
        exp.bullets.add(bullet);
        doc.getExperiences().add(exp);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, candidateProfile, List.of("Kafka"));

        assertTrue(audit.isPassed(), "Legitimate multi-fact synthesis MUST pass audit");
        assertEquals(0, audit.getClaimsFailed());
        assertEquals("READY_FOR_DOWNLOAD", audit.getOverallStatus());

        // Check classification
        boolean hasDerived = audit.getPassedClaims().stream()
                .anyMatch(c -> "DERIVED_FROM_SUPPORTED_FACTS".equals(c.getClassification()) && c.getText().contains("30%"));
        assertTrue(hasDerived, "Synthesized bullet must be classified as DERIVED_FROM_SUPPORTED_FACTS");
    }

    @Test
    @DisplayName("Test C: Unsupported skill in JD (Kafka absent from resume, PASS) - Kafka rejected and omitted, document passes")
    void testC_UnsupportedSkillInJd_KafkaOmitted_Pass() {
        when(jobRepository.findById(targetJob.getId())).thenReturn(Optional.of(targetJob));

        MatchAnalysisResponse match = new MatchAnalysisResponse();
        match.setStrongMatches(List.of("Java", "Spring Boot", "PostgreSQL"));
        when(semanticMatchingService.analyzeAndMatch(targetJob.getId(), candidateUser.getId())).thenReturn(match);

        TailoringPlanDto plan = tailoringService.generateTailoringPlan(targetJob.getId(), candidateUser.getId());

        // Kafka is demanded by target job but candidate lacks evidence
        assertTrue(plan.getRejectedKeywords().stream().anyMatch(r -> r.getKeyword().equalsIgnoreCase("Kafka")),
                "Kafka must be captured in rejected keywords");

        // Verify full tailoring executes without Kafka in document
        when(tailoredResumeRepository.save(any(TailoredResume.class))).thenAnswer(inv -> {
            TailoredResume r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TailoredResumeResponse response = tailoringService.tailorResume(targetJob.getId(), candidateUser.getId());

        assertEquals("READY_FOR_DOWNLOAD", response.getStatus());
        assertTrue(response.getValidationReport().isPassed());
        // Verify Kafka is strictly absent from the tailored output text
        assertFalse(response.getTailoredMarkdown().toLowerCase().contains("kafka"),
                "Tailored resume MUST NOT contain unverified technology Kafka");
    }

    @Test
    @DisplayName("Test D: Fabricated degree (M.Tech invented, BLOCK) - Auditor fails and blocks export when degree is fabricated")
    void testD_FabricatedDegree_Block() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(candidateProfile.getId());
        doc.setJobId(targetJob.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@offerpilot.ai";
        doc.getSummary().text = "Backend Software Engineer specializing in Java, Spring Boot, and PostgreSQL.";

        // Fabricated M.Tech degree not present in candidate records (Candidate only has B.Tech in ECE)
        TailoredResumeDocument.EducationItem fakeEdu = new TailoredResumeDocument.EducationItem();
        fakeEdu.degree = "M.Tech in Computer Science";
        fakeEdu.institution = "IIT Bombay";
        doc.getEducation().add(fakeEdu);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, candidateProfile, List.of());

        assertFalse(audit.isPassed(), "Auditor MUST fail when unverified degree is present");
        assertEquals("VALIDATION_FAILED", audit.getOverallStatus());
        assertTrue(audit.getClaimsFailed() > 0);
        assertTrue(audit.getUnsupportedClaims().stream().anyMatch(c -> c.getClaimType() == ClaimType.DEGREE),
                "Unsupported claims must include DEGREE violation");
    }

    @Test
    @DisplayName("Test E: Fabricated employer (Company B invented, BLOCK) - Auditor fails when unverified employer is inserted")
    void testE_FabricatedEmployer_Block() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(candidateProfile.getId());
        doc.setJobId(targetJob.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@offerpilot.ai";

        // Fabricated employer: Google
        TailoredResumeDocument.ExperienceItem fakeExp = new TailoredResumeDocument.ExperienceItem();
        fakeExp.company = "Google";
        fakeExp.role = "Senior Cloud Architect";
        doc.getExperiences().add(fakeExp);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, candidateProfile, List.of());

        assertFalse(audit.isPassed(), "Auditor MUST block fabricated employer");
        assertEquals("VALIDATION_FAILED", audit.getOverallStatus());
        assertTrue(audit.getUnsupportedClaims().stream()
                .anyMatch(c -> c.getClaimType() == ClaimType.EMPLOYER && c.getText().equalsIgnoreCase("Google")));
    }

    @Test
    @DisplayName("Test F: Fabricated metric (Invented 40% improvement, BLOCK) - Auditor fails when ungrounded percentage is added")
    void testF_FabricatedMetric_Block() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(candidateProfile.getId());
        doc.setJobId(targetJob.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@offerpilot.ai";

        TailoredResumeDocument.ExperienceItem exp = new TailoredResumeDocument.ExperienceItem();
        exp.company = "Razorpay";
        exp.role = "Software Engineer";

        // Candidate only has 30% reduction; this bullet fabricates "40%"
        TailoredResumeDocument.ExperienceBullet fakeMetricBullet = new TailoredResumeDocument.ExperienceBullet();
        fakeMetricBullet.text = "Optimized relational database query execution, slashing latency by 40%.";
        exp.bullets.add(fakeMetricBullet);
        doc.getExperiences().add(exp);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, candidateProfile, List.of());

        assertFalse(audit.isPassed(), "Auditor MUST block ungrounded 40% metric");
        assertEquals("VALIDATION_FAILED", audit.getOverallStatus());
        assertTrue(audit.getUnsupportedClaims().stream()
                .anyMatch(c -> c.getClaimType() == ClaimType.METRIC && c.getText().contains("40%")));
    }

    @Test
    @DisplayName("Test G: Strong truthful rewriting ('Worked on backend APIs' -> 'Developed and maintained backend REST APIs', PASS)")
    void testG_StrongTruthfulRewriting_Pass() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(candidateProfile.getId());
        doc.setJobId(targetJob.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@offerpilot.ai";
        doc.getSummary().text = "Backend Software Engineer with commercial experience in Java, Spring Boot, and PostgreSQL.";

        TailoredResumeDocument.ExperienceItem exp = new TailoredResumeDocument.ExperienceItem();
        exp.company = "Razorpay";
        exp.role = "Software Engineer";

        // Original bullet: "Worked on backend APIs."
        // Rewritten in OfferPilot style: "Developed and maintained backend REST APIs supporting application workflows."
        String rewrittenBullet = "Developed and maintained backend REST APIs supporting application workflows.";
        TailoredResumeDocument.ExperienceBullet bullet = new TailoredResumeDocument.ExperienceBullet(
                rewrittenBullet, razorpayExp.getId(), List.of(razorpayExp.getId()), "DERIVED_FROM_SUPPORTED_FACTS"
        );
        exp.bullets.add(bullet);
        doc.getExperiences().add(exp);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, candidateProfile, List.of());

        assertTrue(audit.isPassed(), "Truthful active engineering rewriting MUST pass");
        assertEquals(0, audit.getClaimsFailed());
        assertEquals("READY_FOR_DOWNLOAD", audit.getOverallStatus());
        assertEquals(ValidationStatus.PASSED, bullet.validationStatus);

        boolean isDerived = audit.getPassedClaims().stream()
                .anyMatch(c -> "DERIVED_FROM_SUPPORTED_FACTS".equals(c.getClassification()) && c.getText().equals(rewrittenBullet));
        assertTrue(isDerived, "Truthfully rewritten bullet must be recorded as DERIVED_FROM_SUPPORTED_FACTS");
    }
}
