package com.jobhunter.service.tailoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.tailoring.TailoringAuditReportDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.model.fact.ClaimType;
import com.jobhunter.model.fact.EvidenceLevel;
import com.jobhunter.model.fact.FactCategory;
import com.jobhunter.model.fact.SkillEvidenceType;
import com.jobhunter.model.tailoring.TailoredResumeDocument;
import com.jobhunter.repository.*;
import com.jobhunter.service.fact.CandidateFactStoreService;
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

class ResumeClaimAuditorTest {

    private CandidateFactRepository factRepository;
    private CandidateExperienceRepository experienceRepository;
    private CandidateProjectRepository projectRepository;
    private CandidateSkillRepository skillRepository;
    private ResumeRepository resumeRepository;
    private ObjectMapper objectMapper;

    private CandidateFactStoreService factStoreService;
    private ResumeClaimAuditor claimAuditor;

    private User testUser;
    private CandidateProfile testProfile;

    @BeforeEach
    void setUp() {
        factRepository = Mockito.mock(CandidateFactRepository.class);
        experienceRepository = Mockito.mock(CandidateExperienceRepository.class);
        projectRepository = Mockito.mock(CandidateProjectRepository.class);
        skillRepository = Mockito.mock(CandidateSkillRepository.class);
        resumeRepository = Mockito.mock(ResumeRepository.class);
        objectMapper = new ObjectMapper();

        when(factRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        factStoreService = new CandidateFactStoreService(
                factRepository,
                experienceRepository,
                projectRepository,
                skillRepository,
                resumeRepository,
                objectMapper
        );

        claimAuditor = new ResumeClaimAuditor(factStoreService);

        testUser = new User("candidate@example.com", "pass", "Jane", "Doe");
        testUser.setId(UUID.randomUUID());

        testProfile = new CandidateProfile();
        testProfile.setId(UUID.randomUUID());
        testProfile.setUser(testUser);
        testProfile.setHeadline("Backend Engineer");
        testProfile.setCurrentLocation("Bengaluru, India");
        testProfile.setPhoneNumber("+91 99999 88888");

        // Verified employer: Razorpay
        CandidateExperience exp = new CandidateExperience(
                testProfile, "Razorpay", "Software Engineer", "2022 - Present",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Built transaction processing service\"]",
                "[\"Reduced API latency by 30%\"]",
                "FINTECH", "Commercial"
        );
        when(experienceRepository.findByCandidateProfileId(testProfile.getId())).thenReturn(List.of(exp));

        // Verified skills: Java, Spring Boot, PostgreSQL
        Skill s1 = new Skill("Java", "LANGUAGE", "[]");
        Skill s2 = new Skill("Spring Boot", "FRAMEWORK", "[]");
        Skill s3 = new Skill("PostgreSQL", "DATABASE", "[]");
        CandidateSkill cs1 = new CandidateSkill(testProfile, s1, "ADVANCED", BigDecimal.valueOf(3), true, "Commercial");
        cs1.setExperienceType("COMMERCIAL");
        CandidateSkill cs2 = new CandidateSkill(testProfile, s2, "ADVANCED", BigDecimal.valueOf(3), true, "Commercial");
        cs2.setExperienceType("COMMERCIAL");
        CandidateSkill cs3 = new CandidateSkill(testProfile, s3, "ADVANCED", BigDecimal.valueOf(3), true, "Commercial");
        cs3.setExperienceType("COMMERCIAL");
        when(skillRepository.findByCandidateProfileId(testProfile.getId())).thenReturn(List.of(cs1, cs2, cs3));
    }

    @Test
    @DisplayName("Education Guard: Candidate with B.Tech ECE must NOT have Master's in CS fabricated, even if JD requires it")
    void testEducationGuardNeverFabricatesDegree() {
        // Candidate verified education: B.Tech in Electronics and Communication Engineering at VIT Vellore
        testProfile.setRawProfileData("{\"education\": [{\"degree\": \"B.Tech in Electronics and Communication Engineering\", \"institution\": \"VIT Vellore\", \"dates\": \"2018 - 2022\"}]}");

        // Suppose document tries to claim Master's in Computer Science because JD asked for it
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";
        doc.getSummary().text = "Software Engineer with experience in Java, Spring Boot, and PostgreSQL.";

        TailoredResumeDocument.EducationItem fabricatedEdu = new TailoredResumeDocument.EducationItem();
        fabricatedEdu.degree = "M.S. in Computer Science";
        fabricatedEdu.institution = "Stanford University";
        doc.getEducation().add(fabricatedEdu);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, testProfile, List.of());

        assertFalse(audit.isPassed(), "Auditor MUST fail when unverified degree is present");
        assertEquals("VALIDATION_FAILED", audit.getOverallStatus());
        assertTrue(audit.getUnsupportedClaims().stream().anyMatch(c -> c.getClaimType() == ClaimType.DEGREE));
    }

    @Test
    @DisplayName("Education Omission: Candidate with NO education on profile must have zero degrees in resume")
    void testEducationOmissionCleanlyOmitted() {
        // Candidate profile has no education
        testProfile.setRawProfileData("{}");

        // Document truthfully contains zero education entries
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";
        doc.getSummary().text = "Software Engineer with experience in Java, Spring Boot, and PostgreSQL.";

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, testProfile, List.of());

        assertTrue(audit.isPassed(), "Auditor should PASS when education is truthfully omitted");
        assertEquals(0, audit.getClaimsFailed());
        assertTrue(audit.getUnsupportedClaims().isEmpty());
    }

    @Test
    @DisplayName("Employer Allowlist: Candidate worked at Razorpay; resume cannot claim Amazon even if in JD")
    void testEmployerAllowlistBlocksFabricatedCompany() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";
        doc.getSummary().text = "Software Engineer with experience in Java, Spring Boot, and PostgreSQL.";

        // Fabricated employer: Amazon
        TailoredResumeDocument.ExperienceItem fakeExp = new TailoredResumeDocument.ExperienceItem();
        fakeExp.company = "Amazon AWS";
        fakeExp.role = "Senior Cloud Architect";
        doc.getExperiences().add(fakeExp);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, testProfile, List.of());

        assertFalse(audit.isPassed());
        assertTrue(audit.getUnsupportedClaims().stream().anyMatch(c -> c.getClaimType() == ClaimType.EMPLOYER && c.getText().contains("Amazon")));
    }

    @Test
    @DisplayName("Skill Containment: Candidate lacks Kafka; Kafka must NOT appear on tailored resume")
    void testSkillContainmentBlocksUnverifiedTech() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";

        // Summary attempts to slip in unverified Kafka
        doc.getSummary().text = "Backend Engineer specializing in Java, Spring Boot, and Kafka distributed event streams.";

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, testProfile, List.of("Kafka"));

        assertFalse(audit.isPassed());
        assertTrue(audit.getUnsupportedClaims().stream().anyMatch(c -> c.getClaimType() == ClaimType.TECHNOLOGY && c.getText().toLowerCase().contains("kafka")));
    }

    @Test
    @DisplayName("Metric Shield: Bullet cannot invent synthetic percentages or latencies not in candidate facts")
    void testMetricShieldBlocksInventedMetrics() {
        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";
        doc.getSummary().text = "Software Engineer with experience in Java, Spring Boot, and PostgreSQL.";

        TailoredResumeDocument.ExperienceItem expItem = new TailoredResumeDocument.ExperienceItem();
        expItem.company = "Razorpay";
        expItem.role = "Software Engineer";

        // Synthetic invented latency: "slashing latency from 14s to 1.8s" (candidate only has 30%)
        TailoredResumeDocument.ExperienceBullet fakeMetricBullet = new TailoredResumeDocument.ExperienceBullet();
        fakeMetricBullet.text = "Optimized query plans, slashing reporting latency from 14s to 1.8s.";
        expItem.bullets.add(fakeMetricBullet);
        doc.getExperiences().add(expItem);

        TailoringAuditReportDto audit = claimAuditor.validateStructuredDocument(doc, testProfile, List.of());

        assertFalse(audit.isPassed());
        assertTrue(audit.getUnsupportedClaims().stream().anyMatch(c -> c.getClaimType() == ClaimType.METRIC));
    }

    @Test
    @DisplayName("Stage 2 Rendered PDF Validation: Catches accidental synthetic text in compiled PDF")
    void testStage2RenderedPdfAudit() throws IOException {
        PdfGenerationService pdfService = new PdfGenerationService(objectMapper);
        testProfile.setRawProfileData("{}"); // Zero education

        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(testProfile.getId());
        doc.getHeader().fullName = "Jane Doe";
        doc.getHeader().email = "candidate@example.com";
        doc.getSummary().text = "Software Engineer with verified Java and PostgreSQL experience.";

        Path tempDir = Files.createTempDirectory("stage2_test_");
        String pdfPath = tempDir.resolve("test_rendered.pdf").toAbsolutePath().toString();

        File pdfFile = pdfService.generatePdfDocumentFromDoc(
                pdfService.generateLatexFromDocument(testUser, testProfile, doc),
                testUser, testProfile, doc, pdfPath
        );

        assertNotNull(pdfFile);
        assertTrue(pdfFile.exists());

        // Stage 2 Audit should pass because PDF has NO fabricated degree and only verified facts
        TailoringAuditReportDto stage2 = claimAuditor.validateRenderedPdf(pdfFile, doc, testProfile, List.of("Kafka", "AWS"));

        assertTrue(stage2.isPassed());
        assertEquals("READY_FOR_DOWNLOAD", stage2.getOverallStatus());
        assertEquals(0, stage2.getClaimsFailed());
    }
}
