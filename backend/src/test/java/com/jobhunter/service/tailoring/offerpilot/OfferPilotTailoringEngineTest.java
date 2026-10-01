package com.jobhunter.service.tailoring.offerpilot;

import com.jobhunter.dto.tailoring.offerpilot.*;
import com.jobhunter.model.entity.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OfferPilotTailoringEngineTest {

    private TailoringGuardrailService guardrailService;
    private OfferPilotLatexRenderer latexRenderer;
    private AtsScoringService atsScoringService;

    @BeforeEach
    void setUp() {
        guardrailService = new TailoringGuardrailService();
        latexRenderer = new OfferPilotLatexRenderer();
        atsScoringService = new AtsScoringService();
    }

    // =========================================================================
    // 1. TEXT EXTRACTION & SANITIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("TextSanitizer: Should strip null bytes, collapse spaces, and normalize newlines")
    void testTextSanitizerSuccess() {
        String raw = "John\u0000 Doe\t\t\fSoftware Engineer\r\n\r\n\r\n\r\nExperience:    Developed REST APIs.\n\n\n\nSkills: Java, Spring Boot, PostgreSQL. Total text must exceed one hundred characters so that validation passes cleanly.";
        String sanitized = ResumeTextSanitizer.sanitize(raw);

        assertFalse(sanitized.contains("\u0000"), "Must not contain null bytes");
        assertFalse(sanitized.contains("\t"), "Must not contain tabs");
        assertFalse(sanitized.contains("\f"), "Must not contain form feeds");
        assertFalse(sanitized.contains("   "), "Must collapse consecutive spaces");
        assertFalse(sanitized.contains("\n\n\n"), "Must collapse 3+ newlines to double newline");
        assertTrue(sanitized.length() >= 100, "Length must be at least 100 characters");
    }

    @Test
    @DisplayName("TextSanitizer: Should throw error when extracted text is less than 100 characters")
    void testTextSanitizerRejectsShortText() {
        String shortText = "Scanned resume image with few words.";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ResumeTextSanitizer.sanitize(shortText)
        );
        assertTrue(ex.getMessage().contains("Could not extract readable text from resume"));
    }

    // =========================================================================
    // 2. PROMPT BUILDER TESTS
    // =========================================================================

    @Test
    @DisplayName("PromptBuilder: Should assemble prompt with 14 critical rules and JSON contract")
    void testPromptBuilder() {
        String prompt = ResumeTailoringPromptBuilder.buildPrompt(
                "Stripe",
                "Senior Backend Engineer",
                List.of("Java", "Spring Boot", "PostgreSQL"),
                List.of("Kafka", "Docker"),
                List.of("Design scalable microservices"),
                List.of("BS in Computer Science"),
                List.of("5+ years experience"),
                List.of("Distributed Systems", "REST"),
                "Candidate master resume text with verified experience in Java and PostgreSQL exceeding 100 characters."
        );

        assertTrue(prompt.contains("CRITICAL RULES:"));
        assertTrue(prompt.contains("1. The master resume is the only source of truth for candidate facts."));
        assertTrue(prompt.contains("12. Do not include missingSkills inside the tailored resume skills section."));
        assertTrue(prompt.contains("Company: Stripe"));
        assertTrue(prompt.contains("Job title: Senior Backend Engineer"));
        assertTrue(prompt.contains("Java, Spring Boot, PostgreSQL"));
        assertTrue(prompt.contains("\"tailoredResume\":"));
        assertTrue(prompt.contains("\"matchedSkills\":"));
        assertTrue(prompt.contains("\"missingSkills\":"));
    }

    // =========================================================================
    // 3. GUARDRAIL & ANTI-HALLUCINATION VERIFICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Guardrail: normalizeSkill correctly preserves c++, c#, .net, node.js")
    void testSkillNormalization() {
        assertEquals("c++", TailoringGuardrailService.normalizeSkill("C++"));
        assertEquals("c#", TailoringGuardrailService.normalizeSkill("  C#  "));
        assertEquals(".net", TailoringGuardrailService.normalizeSkill(".NET"));
        assertEquals("node.js", TailoringGuardrailService.normalizeSkill("Node.js"));
        assertEquals("spring boot", TailoringGuardrailService.normalizeSkill("Spring Boot!!"));
    }

    @Test
    @DisplayName("Guardrail: Fail-fast when missing skill leaks into tailored resume skills")
    void testMissingSkillLeakThrowsException() {
        TailoredResumePayload payload = new TailoredResumePayload();
        payload.getMissingSkills().add(new SkillClassification("Swift", "MISSING", "Not found in resume"));

        // Secretly leaked into programming languages
        payload.getTailoredResume().getSkills().getProgrammingLanguages().add("Swift");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                guardrailService.validateAndAugmentNotes(payload, List.of("Swift"))
        );

        assertTrue(ex.getMessage().contains("Tailored resume contains unsupported missing skill: Swift"));
    }

    @Test
    @DisplayName("Guardrail: Injects missing skills disclosure, required skills warning, and fit warning")
    void testGuardrailProgrammaticNoteAugmentation() {
        TailoredResumePayload payload = new TailoredResumePayload();
        payload.getMatchedSkills().add(new SkillClassification("Java", "MATCHED", "Resume evidence"));
        payload.getMissingSkills().add(new SkillClassification("Swift", "MISSING", "No evidence"));
        payload.getMissingSkills().add(new SkillClassification("iOS", "MISSING", "No evidence"));
        payload.getMissingSkills().add(new SkillClassification("Objective-C", "MISSING", "No evidence"));

        // Missing skills count M=3 >= S=1 -> Triggers fit warning
        guardrailService.validateAndAugmentNotes(payload, List.of("Swift", "iOS"));

        List<String> notes = payload.getTailoringNotes();
        assertFalse(notes.isEmpty());

        // Check disclosure note
        assertTrue(notes.stream().anyMatch(n -> n.contains("Some JD skills were not found in the master resume and were not added")),
                "Must include missing skills disclosure");

        // Check required skills warning
        assertTrue(notes.stream().anyMatch(n -> n.contains("Important: The JD has required skills that were not found in the master resume")),
                "Must include missing required skills warning");

        // Check fit warning heuristic
        assertTrue(notes.stream().anyMatch(n -> n.contains("Fit warning: This role may require several skills not strongly supported")),
                "Must include fit warning note when M >= 3 and M >= S");
    }

    // =========================================================================
    // 4. DETERMINISTIC LATEX RENDERING & ESCAPING TESTS
    // =========================================================================

    @Test
    @DisplayName("LaTeX: Two-stage escaper normalizes typography and escapes TeX control characters")
    void testLatexTwoStageEscaping() {
        String input = "Engineered 100% of C&A’s payment pipeline — reduced latency by 30% with $50k savings & 10# servers #1 {test} 10~20^";
        String escaped = OfferPilotLatexRenderer.escapeLatex(input);

        assertFalse(escaped.contains("% ") && !escaped.contains("\\%"), "Percent must be escaped");
        assertFalse(escaped.contains("& ") && !escaped.contains("\\&"), "Ampersand must be escaped");
        assertFalse(escaped.contains("$") && !escaped.contains("\\$"), "Dollar must be escaped");
        assertFalse(escaped.contains("#") && !escaped.contains("\\#"), "Hash must be escaped");
        assertTrue(escaped.contains("\\%"));
        assertTrue(escaped.contains("\\&"));
        assertTrue(escaped.contains("\\$"));
        assertTrue(escaped.contains("\\#"));
        assertTrue(escaped.contains("\\{test\\}"));
        assertTrue(escaped.contains("---"), "Em-dash must be converted to ---");
        assertTrue(escaped.contains("'"), "Smart apostrophe must be normalized to single quote");
    }

    @Test
    @DisplayName("LaTeX: Renders complete ATS document matching template")
    void testLatexDocumentRendering() {
        TailoredResumePayload.TailoredResumeContent content = new TailoredResumePayload.TailoredResumeContent();
        content.getContactInfo().setFullName("Jane Doe");
        content.getContactInfo().setEmail("jane@example.com");
        content.getContactInfo().setPhone("+1-555-0199");
        content.getContactInfo().setLocation("San Francisco, CA");
        content.getContactInfo().setLinkedinUrl("https://linkedin.com/in/janedoe");

        content.setProfessionalSummary("Senior Backend Engineer specializing in Java and distributed systems.");
        content.getSkills().getProgrammingLanguages().addAll(List.of("Java", "Go"));
        content.getSkills().getDatabases().addAll(List.of("PostgreSQL", "Redis"));

        TailoredResumePayload.ExperienceEntry exp = new TailoredResumePayload.ExperienceEntry();
        exp.setRole("Senior Software Engineer");
        exp.setCompany("Stripe");
        exp.setStartDate("Jan 2021");
        exp.setEndDate("Present");
        exp.getBullets().add("Architected high-throughput payment settlement engine handling 10k TPS.");
        content.getExperience().add(exp);

        content.getEducation().add("B.S. in Computer Science - University of California, Berkeley");

        String latex = latexRenderer.renderLatex(content);

        assertTrue(latex.contains("\\documentclass[10pt,a4paper]{article}"));
        assertTrue(latex.contains("Jane Doe"));
        assertTrue(latex.contains("jane@example.com"));
        assertTrue(latex.contains("\\section*{Professional Summary}"));
        assertTrue(latex.contains("\\section*{Technical Skills}"));
        assertTrue(latex.contains("Programming Languages:"));
        assertTrue(latex.contains("\\section*{Experience}"));
        assertTrue(latex.contains("Senior Software Engineer"));
        assertTrue(latex.contains("Stripe"));
        assertTrue(latex.contains("\\section*{Education}"));
        assertTrue(latex.contains("Berkeley"));
    }

    // =========================================================================
    // 5. CLOSED-LOOP ATS SCORING TESTS
    // =========================================================================

    @Test
    @DisplayName("ATS Scoring: Calculates comparison delta and newly aligned keywords")
    void testClosedLoopAtsScoring() {
        String masterText = "Software engineer working with Java and MySQL. Built basic REST APIs.";
        TailoredResumePayload.TailoredResumeContent tailored = new TailoredResumePayload.TailoredResumeContent();
        tailored.setProfessionalSummary("Software Engineer with Java, Spring Boot, and PostgreSQL production experience.");
        tailored.getSkills().getProgrammingLanguages().add("Java");
        tailored.getSkills().getFrameworks().add("Spring Boot");
        tailored.getSkills().getDatabases().add("PostgreSQL");

        Job job = new Job();
        job.setTitle("Senior Java Engineer");

        List<String> required = List.of("Java", "Spring Boot", "PostgreSQL");
        List<String> preferred = List.of("Docker", "AWS");
        List<String> keywords = List.of("REST", "Microservices");

        AtsComparisonScoreDto comparison = atsScoringService.computeComparison(
                masterText, tailored, job, required, preferred, keywords
        );

        assertTrue(comparison.getTailoredOverallScore() > comparison.getMasterOverallScore(),
                "Tailored overall score should exceed master score");
        assertTrue(comparison.getScoreDelta() > 0, "Score delta must be positive");
        assertNotNull(comparison.getNewlyAlignedKeywords());
    }
}
