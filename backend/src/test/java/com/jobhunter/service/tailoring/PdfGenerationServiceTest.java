package com.jobhunter.service.tailoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.tailoring.BulletTailoringItem;
import com.jobhunter.dto.tailoring.ResumeValidationReport;
import com.jobhunter.dto.tailoring.TailoringPlanDto;
import com.jobhunter.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private PdfGenerationService pdfGenerationService;
    private ObjectMapper objectMapper;

    private User testUser;
    private CandidateProfile testProfile;
    private TailoringPlanDto testPlan;
    private List<CandidateExperience> testExperiences;
    private List<CandidateProject> testProjects;
    private List<CandidateSkill> testSkills;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pdfGenerationService = new PdfGenerationService(objectMapper);

        testUser = new User("alex.engineer@jobhunter.ai", "pass", "Alex", "Dev");
        testUser.setId(UUID.randomUUID());

        testProfile = new CandidateProfile();
        testProfile.setId(UUID.randomUUID());
        testProfile.setUser(testUser);
        testProfile.setHeadline("Software Engineer / Backend Engineer");
        testProfile.setSummary("Software engineer with 2.5 years of experience in Java, Spring Boot, and PostgreSQL.");
        testProfile.setYearsOfExperience(new BigDecimal("2.5"));
        testProfile.setCurrentLocation("Bengaluru, India");
        testProfile.setPhoneNumber("+91 98765 43210");
        testProfile.setLinkedinUrl("https://linkedin.com/in/alex-dev");
        testProfile.setGithubUrl("https://github.com/alex-dev");

        testPlan = new TailoringPlanDto();
        testPlan.setTargetJobTitle("Software Engineer (Java / Spring Boot)");
        testPlan.setTargetCompany("FinTech Solutions");
        testPlan.setTargetRole("Software Engineer");
        testPlan.setOverallStrategy("Position Alex as a disciplined backend engineer specializing in high-throughput Java 17, Spring Boot, and PostgreSQL microservices.");
        testPlan.getSkillsToEmphasize().addAll(List.of("Java", "Spring Boot", "PostgreSQL", "Docker"));
        testPlan.getSkillsToDeemphasize().addAll(List.of("React", "Node.js"));

        testPlan.getBulletSharpeningProposals().add(new BulletTailoringItem(
                "Engineered high-reliability transaction reconciliation REST APIs",
                "Architected resilient, high-throughput REST microservices using Java 17 and Spring Boot for financial transaction reconciliation.",
                "Sharpens alignment with Java 17 and Spring Boot backend requirements.",
                "Java 17, Spring Boot, REST APIs",
                List.of("FinTech Solutions employment record"),
                "VERIFIED_GROUNDED"
        ));

        CandidateExperience exp = new CandidateExperience(
                testProfile,
                "FinTech SaaS Solutions",
                "Software Engineer",
                "Jan 2024 - Present",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Engineered high-reliability transaction reconciliation REST APIs\"]",
                "[\"Reduced batch report generation latency from 14s to 1.8s through query plan optimization\"]",
                "FinTech",
                "Commercial employment"
        );
        testExperiences = List.of(exp);

        CandidateProject proj = new CandidateProject(
                testProfile,
                "Mentor-Mentee Collaboration Platform",
                "Platform connecting engineers",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Docker\"]",
                "Microservice architecture",
                "[\"Designed normalized PostgreSQL database schema\"]",
                "[\"Sustained 100+ concurrent simulated sessions without message loss\"]",
                "GitHub repository",
                "https://github.com/candidate-dev/platform"
        );
        testProjects = List.of(proj);

        Skill javaSkill = new Skill("Java", "LANGUAGE", "[]");
        Skill springSkill = new Skill("Spring Boot", "FRAMEWORK", "[]");
        Skill pgSkill = new Skill("PostgreSQL", "DATABASE", "[]");
        CandidateSkill cs1 = new CandidateSkill(testProfile, javaSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        CandidateSkill cs2 = new CandidateSkill(testProfile, springSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        CandidateSkill cs3 = new CandidateSkill(testProfile, pgSkill, "ADVANCED", new BigDecimal("2.5"), true, "Commercial");
        testSkills = List.of(cs1, cs2, cs3);
    }

    @Test
    @DisplayName("Should generate valid LaTeX source code with escaped characters and no unresolved placeholders")
    void testGenerateLatexSource() {
        String latex = pdfGenerationService.generateLatexSource(
                testUser, testProfile, testPlan, testExperiences, testProjects, testSkills
        );

        assertNotNull(latex);
        assertTrue(latex.contains("Alex Dev"));
        assertTrue(latex.contains("FinTech SaaS Solutions"));
        assertTrue(latex.contains("Software Engineer"));
        assertTrue(latex.contains("PostgreSQL"));

        // Ensure no unresolved mustache tokens remain
        assertFalse(latex.contains("{{NAME}}"));
        assertFalse(latex.contains("{{CONTACT_INFO}}"));
        assertFalse(latex.contains("{{SUMMARY}}"));
        assertFalse(latex.contains("{{SKILLS_SECTION}}"));
        assertFalse(latex.contains("{{EXPERIENCE_SECTION}}"));
        assertFalse(latex.contains("{{PROJECTS_SECTION}}"));
        assertFalse(latex.contains("{{EDUCATION_SECTION}}"));

        // Ensure special chars escaped
        assertFalse(latex.contains("& ") && !latex.contains("\\&"));
    }

    @Test
    @DisplayName("Should compile ATS-compliant PDF document and pass deterministic quality validation")
    void testGeneratePdfAndDeterministicValidation() throws IOException {
        Path tempDir = Files.createTempDirectory("pdf_test_");
        String destPath = tempDir.resolve("tailored_resume_test.pdf").toAbsolutePath().toString();

        String latex = pdfGenerationService.generateLatexSource(
                testUser, testProfile, testPlan, testExperiences, testProjects, testSkills
        );

        File pdfFile = pdfGenerationService.generatePdfDocument(
                latex, testUser, testProfile, testPlan, testExperiences, testProjects, testSkills, destPath
        );

        assertNotNull(pdfFile);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0, "PDF file must not be empty");

        // Validate PDF with PDFBox
        ResumeValidationReport report = pdfGenerationService.validatePdf(pdfFile, "Alex Dev");

        assertNotNull(report);
        assertTrue(report.isPassed(), "Validation report should pass for valid PDF");
        assertTrue(report.getQualityScore() >= 80.0, "Quality score should be >= 80");
        assertTrue(report.getFailedChecks().isEmpty(), "No checks should fail");
        assertTrue(report.getPassedChecks().stream().anyMatch(c -> c.contains("PDF valid format")));
        assertTrue(report.getPassedChecks().stream().anyMatch(c -> c.contains("Candidate name verified")));
        assertTrue(report.getPassedChecks().stream().anyMatch(c -> c.contains("Zero unresolved template placeholders")));
    }

    @Test
    @DisplayName("Should fail validation gracefully if PDF file is corrupt or zero bytes")
    void testPdfValidationFailsOnCorruptFile() throws IOException {
        Path tempDir = Files.createTempDirectory("pdf_fail_test_");
        File emptyFile = tempDir.resolve("empty.pdf").toFile();
        assertTrue(emptyFile.createNewFile());

        ResumeValidationReport report = pdfGenerationService.validatePdf(emptyFile, "Alex Dev");

        assertNotNull(report);
        assertFalse(report.isPassed());
        assertEquals(0.0, report.getQualityScore());
        assertTrue(report.getFailedChecks().stream().anyMatch(f -> f.contains("0 bytes")));

        // Test non-existent file
        File nonExistent = new File(tempDir.toFile(), "ghost.pdf");
        ResumeValidationReport ghostReport = pdfGenerationService.validatePdf(nonExistent, "Alex Dev");
        assertFalse(ghostReport.isPassed());
        assertTrue(ghostReport.getFailedChecks().stream().anyMatch(f -> f.contains("does not exist")));
    }
}
