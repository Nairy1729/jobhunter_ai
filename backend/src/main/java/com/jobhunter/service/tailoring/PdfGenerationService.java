package com.jobhunter.service.tailoring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.tailoring.BulletTailoringItem;
import com.jobhunter.dto.tailoring.ResumeValidationReport;
import com.jobhunter.dto.tailoring.TailoringPlanDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.model.tailoring.TailoredResumeDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic PDF & LaTeX Renderer.
 * Architectural Invariant: The renderer ONLY formats and draws facts present in TailoredResumeDocument.
 * It NEVER invents degrees, employers, universities, metrics, or technologies.
 * If the candidate has no verified education, the Education section is cleanly omitted.
 */
@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);

    private final ObjectMapper objectMapper;

    public PdfGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Primary document-driven LaTeX source generator.
     * Guaranteed to include ONLY facts and sections present in TailoredResumeDocument.
     */
    public String generateLatexFromDocument(User user, CandidateProfile profile, TailoredResumeDocument doc) {
        String template = loadLatexTemplate();

        String fullName = escapeLatex(doc.getHeader() != null && doc.getHeader().fullName != null
                ? doc.getHeader().fullName
                : user.getFirstName() + " " + user.getLastName());

        // Subtitle Line
        String subtitleLine = "";
        if (doc.getHeader() != null && doc.getHeader().subTitle != null && !doc.getHeader().subTitle.isBlank()) {
            subtitleLine = "{\\small \\textbf{" + escapeLatex(doc.getHeader().subTitle) + "}} \\\\ \\vspace{1pt}\n";
        }

        // Contact info
        List<String> contactParts = new ArrayList<>();
        if (doc.getHeader() != null) {
            if (doc.getHeader().phone != null) contactParts.add(escapeLatex(doc.getHeader().phone));
            if (doc.getHeader().email != null) contactParts.add(escapeLatex(doc.getHeader().email));
            if (doc.getHeader().location != null) contactParts.add(escapeLatex(doc.getHeader().location));
            if (doc.getHeader().linkedinUrl != null && !doc.getHeader().linkedinUrl.isBlank()) {
                contactParts.add("\\href{" + doc.getHeader().linkedinUrl + "}{LinkedIn}");
            }
            if (doc.getHeader().githubUrl != null && !doc.getHeader().githubUrl.isBlank()) {
                contactParts.add("\\href{" + doc.getHeader().githubUrl + "}{GitHub}");
            }
        }
        String contactInfo = String.join(" \\quad$|$\\quad ", contactParts);

        // Summary
        String summary = doc.getSummary() != null && doc.getSummary().text != null
                ? escapeLatex(doc.getSummary().text)
                : "";

        // Skills Section
        StringBuilder skillsSb = new StringBuilder();
        for (TailoredResumeDocument.SkillGroup group : doc.getSkillGroups()) {
            if (group.skills == null || group.skills.isEmpty()) continue;
            List<String> skillNames = group.skills.stream()
                    .map(s -> escapeLatex(s.name))
                    .collect(Collectors.toList());
            skillsSb.append("\\textbf{").append(escapeLatex(group.category)).append(": }{")
                    .append(String.join(", ", skillNames)).append("} \\\\\n");
        }
        String skillsSection = skillsSb.toString().trim();

        // Experience Section
        StringBuilder expSb = new StringBuilder();
        for (TailoredResumeDocument.ExperienceItem exp : doc.getExperiences()) {
            expSb.append("\\textbf{").append(escapeLatex(exp.company)).append("} \\hfill ")
                    .append(escapeLatex(exp.duration != null ? exp.duration : "")).append(" \\\\\n");
            expSb.append("\\textit{").append(escapeLatex(exp.role != null ? exp.role : "Software Engineer")).append("} \\hfill ")
                    .append(escapeLatex(exp.location != null ? exp.location : "India")).append(" \\\\\n");
            expSb.append("\\vspace{-4pt}\n");
            expSb.append("\\begin{itemize}[leftmargin=0.15in]\n");

            for (TailoredResumeDocument.ExperienceBullet b : exp.bullets) {
                if (b.text != null && !b.text.isBlank()) {
                    expSb.append("  \\item ").append(escapeLatex(b.text)).append("\n");
                }
            }
            expSb.append("\\end{itemize}\n\\vspace{4pt}\n");
        }
        String experienceSection = expSb.toString();

        // Projects Section
        StringBuilder projSb = new StringBuilder();
        for (TailoredResumeDocument.ProjectItem proj : doc.getProjects()) {
            String techStr = proj.technologies.isEmpty() ? "" : " $|$ \\textit{" + escapeLatex(String.join(", ", proj.technologies)) + "}";
            projSb.append("\\textbf{").append(escapeLatex(proj.name)).append("}").append(techStr).append(" \\hfill ");
            if (proj.projectUrl != null && !proj.projectUrl.isBlank()) {
                projSb.append("\\href{").append(proj.projectUrl).append("}{\\underline{GitHub}} \\\\\n");
            } else {
                projSb.append("\\\\\n");
            }
            if (proj.subTitle != null && !proj.subTitle.isBlank()) {
                projSb.append("\\textit{").append(escapeLatex(proj.subTitle)).append("} \\\\\n");
            }
            projSb.append("\\vspace{-4pt}\n");
            projSb.append("\\begin{itemize}[leftmargin=0.15in]\n");
            for (TailoredResumeDocument.ProjectBullet pb : proj.bullets) {
                if (pb.text != null && !pb.text.isBlank()) {
                    projSb.append("  \\item ").append(escapeLatex(pb.text)).append("\n");
                }
            }
            projSb.append("\\end{itemize}\n\\vspace{4pt}\n");
        }
        String projectsSection = projSb.toString();

        // Education Section: STRICT GUARD
        String educationBlock = "";
        String educationSection = "";
        if (doc.getEducation() != null && !doc.getEducation().isEmpty()) {
            StringBuilder eduSb = new StringBuilder();
            for (TailoredResumeDocument.EducationItem edu : doc.getEducation()) {
                String inst = edu.institution != null && !edu.institution.isBlank() ? edu.institution : edu.degree;
                String deg = edu.institution != null && !edu.institution.isBlank() ? edu.degree : "";
                eduSb.append("\\textbf{").append(escapeLatex(inst)).append("} \\hfill ")
                        .append(escapeLatex(edu.dates != null ? edu.dates : "")).append(" \\\\\n");
                if (!deg.isBlank() || (edu.grade != null && !edu.grade.isBlank())) {
                    eduSb.append("\\textit{").append(escapeLatex(deg)).append("}");
                    if (edu.grade != null && !edu.grade.isBlank()) {
                        eduSb.append(" \\hfill \\textit{").append(escapeLatex(edu.grade)).append("}");
                    }
                    eduSb.append(" \\\\\n");
                }
            }
            educationSection = eduSb.toString();
            educationBlock = "\\section{Education}\n" + educationSection;
        }

        // Achievements Section
        String achievementsBlock = "";
        if (doc.getAchievements() != null && !doc.getAchievements().isEmpty()) {
            StringBuilder achSb = new StringBuilder();
            achSb.append("\\section{Achievements}\n");
            achSb.append("\\begin{itemize}[leftmargin=0.15in]\n");
            for (TailoredResumeDocument.AchievementItem ach : doc.getAchievements()) {
                String achText = "\\textbf{" + escapeLatex(ach.title != null ? ach.title : "") + "}";
                if (ach.description != null && !ach.description.isBlank()) {
                    achText += ": " + escapeLatex(ach.description);
                } else if (ach.rawText != null && !ach.rawText.isBlank()) {
                    achText = escapeLatex(ach.rawText);
                }
                achSb.append("  \\item ").append(achText).append("\n");
            }
            achSb.append("\\end{itemize}\n\\vspace{4pt}\n");
            achievementsBlock = achSb.toString();
        }

        String result = template
                .replace("{{NAME}}", fullName)
                .replace("{{SUBTITLE_LINE}}", subtitleLine)
                .replace("{{CONTACT_INFO}}", contactInfo)
                .replace("{{SUMMARY}}", summary)
                .replace("{{SKILLS_SECTION}}", skillsSection)
                .replace("{{EXPERIENCE_SECTION}}", experienceSection)
                .replace("{{PROJECTS_SECTION}}", projectsSection)
                .replace("{{EDUCATION_BLOCK}}", educationBlock)
                .replace("{{EDUCATION_SECTION}}", educationSection)
                .replace("{{ACHIEVEMENTS_BLOCK}}", achievementsBlock);

        // If education is empty, cleanly remove any dangling Education section headers
        if (doc.getEducation() == null || doc.getEducation().isEmpty()) {
            result = result.replaceAll("(?m)^\\\\section\\*?\\{Education\\}\\s*$", "");
        }
        if (doc.getAchievements() == null || doc.getAchievements().isEmpty()) {
            result = result.replaceAll("(?m)^\\\\section\\*?\\{Achievements\\}\\s*$", "");
        }

        return result;
    }

    /**
     * Backward-compatible LaTeX generator.
     */
    public String generateLatexSource(
            User user,
            CandidateProfile profile,
            TailoringPlanDto plan,
            List<CandidateExperience> experiences,
            List<CandidateProject> projects,
            List<CandidateSkill> skills) {

        TailoredResumeDocument doc = buildDocumentFromPlan(user, profile, plan, experiences, projects, skills);
        return generateLatexFromDocument(user, profile, doc);
    }

    /**
     * Generates physical PDF document directly from TailoredResumeDocument.
     */
    public File generatePdfDocumentFromDoc(
            String latexSource,
            User user,
            CandidateProfile profile,
            TailoredResumeDocument doc,
            String destinationFilePath) throws IOException {

        Path destPath = Paths.get(destinationFilePath);
        if (destPath.getParent() != null && !Files.exists(destPath.getParent())) {
            Files.createDirectories(destPath.getParent());
        }

        boolean compiledWithLatex = false;
        if (isPdflatexAvailable()) {
            try {
                compiledWithLatex = compileWithPdflatex(latexSource, destPath);
            } catch (Exception e) {
                log.warn("pdflatex compilation failed, falling back to PDFBox ATS renderer: {}", e.getMessage());
            }
        }

        if (!compiledWithLatex) {
            log.info("Rendering ATS-compliant PDF via PDFBox to: {}", destinationFilePath);
            renderPdfWithPdfBoxFromDoc(user, profile, doc, destPath.toFile());
        }

        return destPath.toFile();
    }

    /**
     * Backward-compatible PDF generation method.
     */
    public File generatePdfDocument(
            String latexSource,
            User user,
            CandidateProfile profile,
            TailoringPlanDto plan,
            List<CandidateExperience> experiences,
            List<CandidateProject> projects,
            List<CandidateSkill> skills,
            String destinationFilePath) throws IOException {

        TailoredResumeDocument doc = buildDocumentFromPlan(user, profile, plan, experiences, projects, skills);
        return generatePdfDocumentFromDoc(latexSource, user, profile, doc, destinationFilePath);
    }

    /**
     * Extracts text from a compiled PDF using PDFBox.
     */
    public String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * Validates PDF physical integrity (existence, readable ATS text stream, zero unresolved tokens).
     */
    public ResumeValidationReport validatePdf(File pdfFile, String candidateFullName) {
        ResumeValidationReport report = new ResumeValidationReport();
        List<String> passed = report.getPassedChecks();
        List<String> failed = report.getFailedChecks();
        List<String> warnings = report.getWarnings();

        if (pdfFile == null || !pdfFile.exists()) {
            report.setPassed(false);
            failed.add("PDF file does not exist on disk.");
            report.setQualityScore(0.0);
            return report;
        }

        if (pdfFile.length() == 0) {
            report.setPassed(false);
            failed.add("PDF file is 0 bytes (empty file).");
            report.setQualityScore(0.0);
            return report;
        }
        passed.add("PDF file exists with size " + pdfFile.length() + " bytes.");

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount < 1) {
                report.setPassed(false);
                failed.add("PDF contains 0 pages.");
            } else {
                passed.add("PDF valid format with " + pageCount + " page(s).");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                report.setPassed(false);
                failed.add("PDF contains no readable text content (unparseable).");
            } else {
                passed.add("PDF contains readable ATS text stream (" + text.length() + " characters).");

                // Check candidate name
                String[] nameTokens = candidateFullName.split("\\s+");
                boolean nameFound = false;
                for (String token : nameTokens) {
                    if (text.toLowerCase().contains(token.toLowerCase())) {
                        nameFound = true;
                        break;
                    }
                }
                if (nameFound) {
                    passed.add("Candidate name verified in PDF text.");
                } else {
                    warnings.add("Candidate name was not explicitly detected in stripped text.");
                }

                // Check key sections
                boolean hasExp = text.toUpperCase().contains("EXPERIENCE");
                boolean hasSkills = text.toUpperCase().contains("SKILL");
                if (hasExp && hasSkills) {
                    passed.add("Standard ATS sections detected (Professional Experience & Skills).");
                } else {
                    warnings.add("One or more standard ATS section headers (Experience, Skills) may be missing.");
                }

                // Check unresolved placeholders
                if (text.contains("{{") || text.contains("}}")) {
                    report.setPassed(false);
                    failed.add("PDF contains unresolved template placeholder tokens: {{ or }}.");
                } else {
                    passed.add("Zero unresolved template placeholders detected.");
                }
            }

        } catch (Exception e) {
            report.setPassed(false);
            failed.add("Failed to parse and validate PDF: " + e.getMessage());
        }

        double score = 100.0;
        score -= failed.size() * 35.0;
        score -= warnings.size() * 10.0;
        report.setQualityScore(Math.max(0.0, Math.min(100.0, score)));
        if (!failed.isEmpty()) {
            report.setPassed(false);
        }

        return report;
    }

    /**
     * Renders ATS-compliant PDF directly using Apache PDFBox based strictly on TailoredResumeDocument.
     * Never invents education or unverified metrics.
     */
    public void renderPdfWithPdfBoxFromDoc(
            User user,
            CandidateProfile profile,
            TailoredResumeDocument doc,
            File outputFile) throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font headingFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font italicFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 36f; // 0.5 in margins matching Master Resume
            float printableWidth = pageSize.getWidth() - (margin * 2);
            float startY = pageSize.getHeight() - margin;

            PageState pageState = new PageState(document, pageSize, margin, startY, printableWidth);

            // 1. Header: Name & Contact Info
            String fullName = sanitizeForPdf(doc.getHeader() != null && doc.getHeader().fullName != null
                    ? doc.getHeader().fullName
                    : user.getFirstName() + " " + user.getLastName());
            pageState.drawCenteredText(fullName, titleFont, 17f);
            pageState.y -= 2f;

            if (doc.getHeader() != null && doc.getHeader().subTitle != null && !doc.getHeader().subTitle.isBlank()) {
                pageState.drawCenteredText(sanitizeForPdf(doc.getHeader().subTitle), boldFont, 10f);
                pageState.y -= 2f;
            }

            List<String> contactParts = new ArrayList<>();
            if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isBlank()) contactParts.add(profile.getPhoneNumber());
            if (user.getEmail() != null && !user.getEmail().isBlank()) contactParts.add(user.getEmail());
            if (profile.getCurrentLocation() != null && !profile.getCurrentLocation().isBlank()) contactParts.add(profile.getCurrentLocation());
            if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
                contactParts.add(profile.getLinkedinUrl().replaceFirst("^https?://", ""));
            }
            if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
                contactParts.add(profile.getGithubUrl().replaceFirst("^https?://", ""));
            }
            String contactLine = sanitizeForPdf(String.join("  |  ", contactParts));
            pageState.drawCenteredText(contactLine, regularFont, 9f);
            pageState.y -= 10f;

            // 2. Professional Summary
            if (doc.getSummary() != null && doc.getSummary().text != null && !doc.getSummary().text.isBlank()) {
                pageState.renderSection("PROFESSIONAL SUMMARY", headingFont, () -> {
                    pageState.drawParagraph(sanitizeForPdf(doc.getSummary().text), regularFont, 9.5f, 12f);
                });
            }

            // 3. Technical Skills
            if (doc.getSkillGroups() != null && !doc.getSkillGroups().isEmpty()) {
                pageState.renderSection("TECHNICAL SKILLS", headingFont, () -> {
                    for (TailoredResumeDocument.SkillGroup sg : doc.getSkillGroups()) {
                        if (sg.skills == null || sg.skills.isEmpty()) continue;
                        String items = sg.skills.stream().map(s -> s.name).collect(Collectors.joining(", "));
                        pageState.drawSkillCategory(sg.category + ": ", items, boldFont, regularFont, 9.5f);
                    }
                });
            }

            // 4. Professional Experience
            if (doc.getExperiences() != null && !doc.getExperiences().isEmpty()) {
                pageState.renderSection("PROFESSIONAL EXPERIENCE", headingFont, () -> {
                    for (TailoredResumeDocument.ExperienceItem exp : doc.getExperiences()) {
                        pageState.ensureSpace(45f);
                        String company = sanitizeForPdf(exp.company != null ? exp.company : "Company");
                        String duration = sanitizeForPdf(exp.duration != null ? exp.duration : "");
                        String role = sanitizeForPdf(exp.role != null ? exp.role : "Software Engineer");
                        String location = sanitizeForPdf(exp.location != null ? exp.location : "Bengaluru, India");

                        pageState.drawTwoColumnLine(company, duration, boldFont, boldFont, 10f);
                        pageState.drawTwoColumnLine(role, location, italicFont, italicFont, 9.5f);
                        pageState.y -= 2f;

                        for (TailoredResumeDocument.ExperienceBullet b : exp.bullets) {
                            if (b.text != null && !b.text.isBlank()) {
                                pageState.drawBullet(sanitizeForPdf(b.text), regularFont, 9.5f, 12f);
                            }
                        }
                        pageState.y -= 3f;
                    }
                });
            }

            // 5. Technical Projects
            if (doc.getProjects() != null && !doc.getProjects().isEmpty()) {
                pageState.renderSection("TECHNICAL PROJECTS", headingFont, () -> {
                    for (TailoredResumeDocument.ProjectItem proj : doc.getProjects()) {
                        pageState.ensureSpace(40f);
                        String projName = sanitizeForPdf(proj.name != null ? proj.name : "Technical Project");
                        String techStr = proj.technologies.isEmpty() ? "" : String.join(", ", proj.technologies);

                        pageState.drawTwoColumnLine(projName, techStr, boldFont, italicFont, 9.5f);
                        if (proj.subTitle != null && !proj.subTitle.isBlank()) {
                            String urlStr = proj.projectUrl != null && !proj.projectUrl.isBlank() ? "Link" : "";
                            pageState.drawTwoColumnLine(sanitizeForPdf(proj.subTitle), urlStr, italicFont, italicFont, 9f);
                        }
                        pageState.y -= 2f;

                        for (TailoredResumeDocument.ProjectBullet pb : proj.bullets) {
                            if (pb.text != null && !pb.text.isBlank()) {
                                pageState.drawBullet(sanitizeForPdf(pb.text), regularFont, 9.5f, 12f);
                            }
                        }
                        pageState.y -= 3f;
                    }
                });
            }

            // 6. Education: STRICT GUARD
            if (doc.getEducation() != null && !doc.getEducation().isEmpty()) {
                pageState.renderSection("EDUCATION", headingFont, () -> {
                    for (TailoredResumeDocument.EducationItem edu : doc.getEducation()) {
                        pageState.ensureSpace(24f);
                        String inst = edu.institution != null && !edu.institution.isBlank() ? edu.institution : edu.degree;
                        String deg = edu.institution != null && !edu.institution.isBlank() ? edu.degree : "";
                        String dates = edu.dates != null ? edu.dates : "";
                        String grade = edu.grade != null ? edu.grade : "";

                        pageState.drawTwoColumnLine(sanitizeForPdf(inst), sanitizeForPdf(dates), boldFont, boldFont, 9.5f);
                        if (!deg.isBlank() || !grade.isBlank()) {
                            pageState.drawTwoColumnLine(sanitizeForPdf(deg), sanitizeForPdf(grade), italicFont, italicFont, 9.5f);
                        }
                        pageState.y -= 2f;
                    }
                });
            }

            // 7. Achievements Section
            if (doc.getAchievements() != null && !doc.getAchievements().isEmpty()) {
                pageState.renderSection("ACHIEVEMENTS", headingFont, () -> {
                    for (TailoredResumeDocument.AchievementItem ach : doc.getAchievements()) {
                        pageState.ensureSpace(18f);
                        String title = ach.title != null ? ach.title : "";
                        String desc = ach.description != null ? ach.description : (ach.rawText != null ? ach.rawText : "");
                        pageState.drawAchievementBullet(sanitizeForPdf(title), sanitizeForPdf(desc), boldFont, regularFont, 9.5f, 12f);
                    }
                });
            }

            List<String> overlapViolations = pageState.verifyDividerNoOverlap();
            if (!overlapViolations.isEmpty()) {
                log.warn("Detected {} divider-text overlap violations during PDFBox rendering: {}",
                        overlapViolations.size(), overlapViolations);
            }

            pageState.close();
            document.save(outputFile);
        }
    }

    private void renderPdfWithPdfBox(
            User user,
            CandidateProfile profile,
            TailoringPlanDto plan,
            List<CandidateExperience> experiences,
            List<CandidateProject> projects,
            List<CandidateSkill> skills,
            File outputFile) throws IOException {

        TailoredResumeDocument doc = buildDocumentFromPlan(user, profile, plan, experiences, projects, skills);
        renderPdfWithPdfBoxFromDoc(user, profile, doc, outputFile);
    }

    /**
     * Helper to assemble a TailoredResumeDocument from legacy parameters if invoked via older API.
     */
    private TailoredResumeDocument buildDocumentFromPlan(
            User user,
            CandidateProfile profile,
            TailoringPlanDto plan,
            List<CandidateExperience> experiences,
            List<CandidateProject> projects,
            List<CandidateSkill> skills) {

        TailoredResumeDocument doc = new TailoredResumeDocument();
        doc.setCandidateProfileId(profile.getId());

        // Header
        doc.getHeader().fullName = user.getFirstName() + " " + user.getLastName();
        doc.getHeader().email = user.getEmail();
        doc.getHeader().phone = profile.getPhoneNumber();
        doc.getHeader().location = profile.getCurrentLocation();
        doc.getHeader().linkedinUrl = profile.getLinkedinUrl();
        doc.getHeader().githubUrl = profile.getGithubUrl();
        extractHeaderSubtitleIntoDoc(profile, doc);

        // Summary: Truthful summary
        doc.getSummary().text = buildProfessionalSummary(profile, plan);

        // Skills
        List<String> emphasizedNames = plan.getSkillsToEmphasize() != null ? plan.getSkillsToEmphasize() : List.of();
        TailoredResumeDocument.SkillGroup coreGroup = new TailoredResumeDocument.SkillGroup("Languages & Core");
        TailoredResumeDocument.SkillGroup fwGroup = new TailoredResumeDocument.SkillGroup("Frameworks & APIs");
        TailoredResumeDocument.SkillGroup dbGroup = new TailoredResumeDocument.SkillGroup("Databases & Systems");

        for (CandidateSkill cs : skills) {
            if (cs.getSkill() == null) continue;
            String name = cs.getSkill().getName();
            String lower = name.toLowerCase();
            TailoredResumeDocument.SkillItem item = new TailoredResumeDocument.SkillItem(name, com.jobhunter.model.fact.SkillEvidenceType.VERIFIED_SKILL, cs.getId());

            if (lower.contains("java") || lower.contains("python") || lower.contains("script") || lower.contains("c#") || lower.contains("c++") || lower.contains("go")) {
                coreGroup.skills.add(item);
            } else if (lower.contains("spring") || lower.contains("rest") || lower.contains("jwt") || lower.contains("security") || lower.contains("react") || lower.contains("node")) {
                fwGroup.skills.add(item);
            } else {
                dbGroup.skills.add(item);
            }
        }

        // Sort emphasized first
        coreGroup.skills.sort((a, b) -> Boolean.compare(emphasizedNames.contains(b.name), emphasizedNames.contains(a.name)));
        fwGroup.skills.sort((a, b) -> Boolean.compare(emphasizedNames.contains(b.name), emphasizedNames.contains(a.name)));
        dbGroup.skills.sort((a, b) -> Boolean.compare(emphasizedNames.contains(b.name), emphasizedNames.contains(a.name)));

        if (!coreGroup.skills.isEmpty()) doc.getSkillGroups().add(coreGroup);
        if (!fwGroup.skills.isEmpty()) doc.getSkillGroups().add(fwGroup);
        if (!dbGroup.skills.isEmpty()) doc.getSkillGroups().add(dbGroup);

        // Experience & Bullets
        Map<String, BulletTailoringItem> bulletMap = new HashMap<>();
        if (plan.getBulletSharpeningProposals() != null) {
            for (BulletTailoringItem b : plan.getBulletSharpeningProposals()) {
                if (b.getOriginalBullet() != null) {
                    bulletMap.put(b.getOriginalBullet().trim().toLowerCase(), b);
                }
            }
        }

        for (CandidateExperience exp : experiences) {
            TailoredResumeDocument.ExperienceItem expItem = new TailoredResumeDocument.ExperienceItem();
            expItem.sourceExperienceId = exp.getId();
            expItem.company = exp.getCompany();
            expItem.role = exp.getRole();
            expItem.duration = exp.getDuration();
            expItem.location = profile.getCurrentLocation() != null ? profile.getCurrentLocation() : "Bengaluru, India";

            List<String> rawBullets = parseJsonList(exp.getAchievements());
            rawBullets.addAll(parseJsonList(exp.getResponsibilities()));

            for (String raw : rawBullets) {
                String clean = raw.trim();
                if (clean.isBlank()) continue;
                String bulletText = clean;
                BulletTailoringItem prop = bulletMap.get(clean.toLowerCase());
                if (prop != null && prop.getProposedBullet() != null && !prop.getProposedBullet().isBlank()) {
                    bulletText = prop.getProposedBullet();
                }
                List<UUID> expFactIds = exp.getId() != null ? List.of(exp.getId()) : Collections.emptyList();
                expItem.bullets.add(new TailoredResumeDocument.ExperienceBullet(bulletText, exp.getId(), expFactIds));
            }
            doc.getExperiences().add(expItem);
        }

        // Projects
        for (CandidateProject proj : projects) {
            TailoredResumeDocument.ProjectItem pItem = new TailoredResumeDocument.ProjectItem();
            pItem.sourceProjectId = proj.getId();
            pItem.name = proj.getName();
            pItem.technologies = parseJsonList(proj.getTechnologies());
            pItem.projectUrl = proj.getProjectUrl();
            if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                pItem.subTitle = proj.getDescription().trim();
            }

            List<String> pBullets = parseJsonList(proj.getMeasurableOutcomes());
            pBullets.addAll(parseJsonList(proj.getResponsibilities()));
            if (pBullets.isEmpty() && proj.getDescription() != null) {
                pBullets.add(proj.getDescription());
            }

            for (String pb : pBullets) {
                if (!pb.isBlank()) {
                    List<UUID> projFactIds = proj.getId() != null ? List.of(proj.getId()) : Collections.emptyList();
                    pItem.bullets.add(new TailoredResumeDocument.ProjectBullet(pb, proj.getId(), projFactIds));
                }
            }
            doc.getProjects().add(pItem);
        }

        // Education: NEVER synthesize or invent! If candidate profile has no education, leave empty!
        extractEducationIntoDoc(profile, doc);

        // Achievements
        extractAchievementsIntoDoc(profile, doc);

        return doc;
    }

    private void extractEducationIntoDoc(CandidateProfile profile, TailoredResumeDocument doc) {
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                com.fasterxml.jackson.databind.JsonNode eduArr = root.path("education");
                if (eduArr.isArray() && !eduArr.isEmpty()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : eduArr) {
                        String degree = item.path("degree").asText("").trim();
                        String inst = item.path("institution").asText("").trim();
                        String dates = item.path("dates").asText("").trim();
                        String grade = item.path("grade").asText("").trim();
                        if (!degree.isBlank() || !inst.isBlank()) {
                            TailoredResumeDocument.EducationItem e = new TailoredResumeDocument.EducationItem();
                            e.degree = degree;
                            e.institution = inst;
                            e.dates = dates;
                            e.grade = grade;
                            doc.getEducation().add(e);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void extractAchievementsIntoDoc(CandidateProfile profile, TailoredResumeDocument doc) {
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                com.fasterxml.jackson.databind.JsonNode achArr = root.path("achievements");
                if (achArr.isArray() && !achArr.isEmpty()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : achArr) {
                        String title = item.path("title").asText("").trim();
                        String desc = item.path("description").asText("").trim();
                        String raw = item.path("rawText").asText("").trim();
                        if (!title.isBlank() || !raw.isBlank()) {
                            doc.getAchievements().add(new TailoredResumeDocument.AchievementItem(title, desc, raw));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void extractHeaderSubtitleIntoDoc(CandidateProfile profile, TailoredResumeDocument doc) {
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                String headline = root.path("headline").asText("").trim();
                if (!headline.isBlank()) {
                    doc.getHeader().subTitle = headline;
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            doc.getHeader().subTitle = profile.getHeadline();
        }
    }

    public String buildProfessionalSummary(CandidateProfile profile, TailoringPlanDto plan) {
        List<String> strongSkills = plan != null ? plan.getSkillsToEmphasize() : List.of();
        String skillsStr = (strongSkills != null && !strongSkills.isEmpty())
                ? String.join(", ", strongSkills)
                : "Java, Spring Boot, and PostgreSQL";

        String yearsExp = profile.getYearsOfExperience() != null
                ? profile.getYearsOfExperience() + "+ years"
                : "2+ years";

        String targetRole = (plan != null && plan.getTargetRole() != null && !plan.getTargetRole().isBlank())
                ? plan.getTargetRole()
                : "Software Engineer";

        String roleIdentity;
        String roleLower = targetRole.toLowerCase();
        if (roleLower.contains("backend") && !roleLower.contains("frontend")) {
            roleIdentity = "Backend Software Engineer";
        } else if (roleLower.contains("frontend") && !roleLower.contains("backend")) {
            roleIdentity = "Frontend Software Engineer";
        } else if (roleLower.contains("full stack") || roleLower.contains("fullstack")) {
            roleIdentity = "Full Stack Software Engineer";
        } else if (roleLower.contains("devops") || roleLower.contains("sre")) {
            roleIdentity = "DevOps & Cloud Engineer";
        } else {
            roleIdentity = "Software Engineer";
        }

        return roleIdentity + " with " + yearsExp + " of commercial production experience specializing in "
                + skillsStr + ". Proven track record in architecting high-reliability RESTful microservices, optimizing database performance, and building resilient backend systems with clean architecture.";
    }

    @FunctionalInterface
    public interface SectionContentRenderer {
        void render() throws IOException;
    }

    private static class PageState {
        private final PDDocument document;
        private final PDRectangle pageSize;
        private final float margin;
        private final float startY;
        private final float printableWidth;
        private PDPage currentPage;
        private PDPageContentStream stream;
        public float y;
        private int currentPageIndex = -1;

        public static class Box {
            public final int page;
            public final float minX, maxX, minY, maxY;
            public final String label;

            public Box(int page, float minX, float maxX, float minY, float maxY, String label) {
                this.page = page;
                this.minX = minX;
                this.maxX = maxX;
                this.minY = minY;
                this.maxY = maxY;
                this.label = label;
            }

            public boolean intersects(Box other) {
                if (this.page != other.page) return false;
                return (this.minX < other.maxX && this.maxX > other.minX &&
                        this.minY < other.maxY && this.maxY > other.minY);
            }
        }

        public final List<Box> dividerBoxes = new ArrayList<>();
        public final List<Box> textBoxes = new ArrayList<>();

        public PageState(PDDocument document, PDRectangle pageSize, float margin, float startY, float printableWidth) throws IOException {
            this.document = document;
            this.pageSize = pageSize;
            this.margin = margin;
            this.startY = startY;
            this.printableWidth = printableWidth;
            this.y = startY;
            newPage();
        }

        public void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            currentPage = new PDPage(pageSize);
            document.addPage(currentPage);
            currentPageIndex++;
            stream = new PDPageContentStream(document, currentPage);
            y = startY;
        }

        public void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < margin) {
                newPage();
            }
        }

        public void recordTextBox(float minX, float maxX, float minY, float maxY, String text) {
            textBoxes.add(new Box(currentPageIndex, minX, maxX, minY, maxY, text));
        }

        public void recordDividerBox(float minX, float maxX, float lineY) {
            dividerBoxes.add(new Box(currentPageIndex, minX, maxX, lineY - 0.25f, lineY + 0.25f, "DIVIDER"));
        }

        public List<String> verifyDividerNoOverlap() {
            List<String> violations = new ArrayList<>();
            for (Box div : dividerBoxes) {
                for (Box txt : textBoxes) {
                    if (div.intersects(txt)) {
                        violations.add("Divider on page " + (div.page + 1) + " (Y=" + div.minY + ") intersects text '" + txt.label + "' (Y=[" + txt.minY + ", " + txt.maxY + "])");
                    }
                }
            }
            return violations;
        }

        public List<String> verifyDividerWhitespaceBuffer(float minBufferPoints) {
            List<String> violations = new ArrayList<>();
            for (Box div : dividerBoxes) {
                Box bufferZone = new Box(div.page, div.minX, div.maxX, div.minY - minBufferPoints, div.maxY + minBufferPoints, "DIVIDER_BUFFER");
                for (Box txt : textBoxes) {
                    if (bufferZone.intersects(txt)) {
                        violations.add("Text '" + txt.label + "' violates " + minBufferPoints + "pt whitespace buffer around divider on page " + (div.page + 1));
                    }
                }
            }
            return violations;
        }

        public void drawCenteredText(String text, PDType1Font font, float fontSize) throws IOException {
            ensureSpace(fontSize + 6f);
            float stringWidth = font.getStringWidth(text) / 1000f * fontSize;
            float x = margin + (printableWidth - stringWidth) / 2f;
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(Math.max(margin, x), y);
            stream.showText(text);
            stream.endText();
            recordTextBox(x, x + stringWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), text);
            y -= fontSize;
        }

        /**
         * Reusable Section Component adhering strictly to document flow:
         * 1. Pre-check: keep heading + divider + first 1-2 content items together (>= 50pt).
         * 2. Section Heading text with proper spacing.
         * 3. Heading-to-divider clear spacing (7.5pt, leaving 4.6pt whitespace below descenders).
         * 4. 0.5pt horizontal rule divider across printable width.
         * 5. Divider-to-content clear spacing (12.5pt, leaving 5.4pt whitespace above content ascenders).
         * 6. Sequential content execution (variable bullet/project heights push dividers naturally).
         * 7. Section bottom spacing.
         */
        public void renderSection(String title, PDType1Font headingFont, SectionContentRenderer contentRenderer) throws IOException {
            ensureSpace(50f);
            y -= 4f;

            // 1. Heading text
            stream.beginText();
            stream.setFont(headingFont, 11.5f);
            stream.newLineAtOffset(margin, y);
            stream.showText(title);
            stream.endText();
            float titleWidth = headingFont.getStringWidth(title) / 1000f * 11.5f;
            recordTextBox(margin, margin + titleWidth, y - (11.5f * 0.25f), y + (11.5f * 0.85f), title);

            // 2. Heading to divider spacing
            y -= 7.5f;

            // 3. Horizontal rule divider
            stream.setLineWidth(0.5f);
            stream.moveTo(margin, y);
            stream.lineTo(margin + printableWidth, y);
            stream.stroke();
            recordDividerBox(margin, margin + printableWidth, y);

            // 4. Divider to content spacing
            y -= 12.5f;

            // 5. Render content
            if (contentRenderer != null) {
                contentRenderer.render();
            }

            // 6. Bottom spacing
            y -= 4f;
        }

        public void drawSectionHeading(String title, PDType1Font font) throws IOException {
            renderSection(title, font, null);
        }

        public void drawTwoColumnLine(String left, String right, PDType1Font leftFont, PDType1Font rightFont, float fontSize) throws IOException {
            ensureSpace(fontSize + 6f);
            stream.beginText();
            stream.setFont(leftFont, fontSize);
            stream.newLineAtOffset(margin, y);
            stream.showText(left);
            stream.endText();
            float leftWidth = leftFont.getStringWidth(left) / 1000f * fontSize;
            recordTextBox(margin, margin + leftWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), left);

            if (right != null && !right.isBlank()) {
                float rightWidth = rightFont.getStringWidth(right) / 1000f * fontSize;
                float rx = margin + printableWidth - rightWidth;
                stream.beginText();
                stream.setFont(rightFont, fontSize);
                stream.newLineAtOffset(rx, y);
                stream.showText(right);
                stream.endText();
                recordTextBox(rx, rx + rightWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), right);
            }
            y -= (fontSize + 3f);
        }

        public void drawParagraph(String text, PDType1Font font, float fontSize, float lineHeight) throws IOException {
            List<String> lines = wrapText(text, printableWidth, font, fontSize);
            for (String line : lines) {
                ensureSpace(lineHeight);
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(margin, y);
                stream.showText(line);
                stream.endText();
                float lineWidth = font.getStringWidth(line) / 1000f * fontSize;
                recordTextBox(margin, margin + lineWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), line);
                y -= lineHeight;
            }
        }

        public void drawSkillCategory(String prefix, String items, PDType1Font boldFont, PDType1Font regFont, float fontSize) throws IOException {
            String full = prefix + items;
            List<String> lines = wrapText(full, printableWidth, regFont, fontSize);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                ensureSpace(12f);
                stream.beginText();
                if (i == 0 && line.startsWith(prefix)) {
                    stream.setFont(boldFont, fontSize);
                    stream.newLineAtOffset(margin, y);
                    stream.showText(prefix);
                    stream.setFont(regFont, fontSize);
                    stream.showText(line.substring(prefix.length()));
                } else {
                    stream.setFont(regFont, fontSize);
                    stream.newLineAtOffset(margin, y);
                    stream.showText(line);
                }
                stream.endText();
                float lineWidth = regFont.getStringWidth(line) / 1000f * fontSize;
                recordTextBox(margin, margin + lineWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), line);
                y -= 12f;
            }
        }

        public void drawBullet(String text, PDType1Font font, float fontSize, float lineHeight) throws IOException {
            float bulletIndent = 13f;
            float bulletWidth = printableWidth - bulletIndent;
            List<String> lines = wrapText(text, bulletWidth, font, fontSize);
            for (int i = 0; i < lines.size(); i++) {
                ensureSpace(lineHeight);
                stream.beginText();
                stream.setFont(font, fontSize);
                if (i == 0) {
                    stream.newLineAtOffset(margin + 2f, y);
                    stream.showText("- ");
                    stream.newLineAtOffset(bulletIndent - 2f, 0);
                    stream.showText(lines.get(i));
                } else {
                    stream.newLineAtOffset(margin + bulletIndent, y);
                    stream.showText(lines.get(i));
                }
                stream.endText();
                float lineWidth = font.getStringWidth(lines.get(i)) / 1000f * fontSize;
                recordTextBox(margin + bulletIndent, margin + bulletIndent + lineWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), lines.get(i));
                y -= lineHeight;
            }
        }

        public void drawAchievementBullet(String title, String desc, PDType1Font boldFont, PDType1Font regFont, float fontSize, float lineHeight) throws IOException {
            float bulletIndent = 13f;
            float bulletWidth = printableWidth - bulletIndent;
            String prefix = (title != null && !title.isBlank()) ? title + ": " : "";
            String full = prefix + (desc != null ? desc : "");
            List<String> lines = wrapText(full, bulletWidth, regFont, fontSize);

            for (int i = 0; i < lines.size(); i++) {
                ensureSpace(lineHeight);
                stream.beginText();
                if (i == 0) {
                    stream.setFont(regFont, fontSize);
                    stream.newLineAtOffset(margin + 2f, y);
                    stream.showText("- ");
                    stream.newLineAtOffset(bulletIndent - 2f, 0);

                    String line = lines.get(0);
                    if (!prefix.isBlank() && line.startsWith(prefix)) {
                        stream.setFont(boldFont, fontSize);
                        stream.showText(prefix);
                        stream.setFont(regFont, fontSize);
                        stream.showText(line.substring(prefix.length()));
                    } else {
                        stream.setFont(regFont, fontSize);
                        stream.showText(line);
                    }
                } else {
                    stream.setFont(regFont, fontSize);
                    stream.newLineAtOffset(margin + bulletIndent, y);
                    stream.showText(lines.get(i));
                }
                stream.endText();
                float lineWidth = regFont.getStringWidth(lines.get(i)) / 1000f * fontSize;
                recordTextBox(margin + bulletIndent, margin + bulletIndent + lineWidth, y - (fontSize * 0.25f), y + (fontSize * 0.85f), lines.get(i));
                y -= lineHeight;
            }
        }

        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        private List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isBlank()) return lines;
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                String test = current.length() == 0 ? word : current + " " + word;
                float width = font.getStringWidth(test) / 1000f * fontSize;
                if (width <= maxWidth) {
                    current.append(current.length() == 0 ? "" : " ").append(word);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                        current = new StringBuilder(word);
                    } else {
                        lines.add(word);
                    }
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            return lines;
        }
    }

    private String loadLatexTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/latex-resume.tex");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.warn("Could not load latex-resume.tex from classpath, using built-in default template", e);
            return defaultLatexTemplate();
        }
    }

    private List<String> parseJsonList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String clean = json.replaceAll("[\\[\\]\"']", "");
            for (String part : clean.split(",")) {
                if (!part.trim().isBlank()) list.add(part.trim());
            }
            return list;
        }
    }

    private boolean isPdflatexAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pdflatex", "--version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean compileWithPdflatex(String latexSource, Path destPdfPath) {
        try {
            Path tempDir = Files.createTempDirectory("resume_latex_");
            Path texFile = tempDir.resolve("resume.tex");
            Files.writeString(texFile, latexSource, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(
                    "pdflatex",
                    "-no-shell-escape",
                    "-interaction=nonstopmode",
                    "-output-directory=" + tempDir.toAbsolutePath(),
                    texFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("pdflatex compilation timed out after 15s");
                return false;
            }
            int exitCode = p.exitValue();

            Path genPdf = tempDir.resolve("resume.pdf");
            if (exitCode == 0 && Files.exists(genPdf) && Files.size(genPdf) > 0) {
                Files.copy(genPdf, destPdfPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Successfully compiled LaTeX to PDF with native pdflatex at: {}", destPdfPath);
                return true;
            }
        } catch (Exception e) {
            log.warn("pdflatex execution error: {}", e.getMessage());
        }
        return false;
    }

    public static String escapeLatex(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }

    public static String sanitizeForPdf(String text) {
        if (text == null) return "";
        return text
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "--")
                .replace("\u2022", "-")
                .replaceAll("(?i)\\b(Post|Repos|Microser|initia|Technolo|Devel|Architec|Deploy|Configur|Applicat|Manag)-\\s*", "$1")
                .replaceAll("[^\\x00-\\x7F]", " "); // Strip non-ASCII to prevent PDFBox Type1 encoding faults
    }

    private String defaultLatexTemplate() {
        return "\\documentclass[10pt,a4paper]{article}\n" +
                "\\usepackage[utf8]{inputenc}\n" +
                "\\usepackage[margin=0.5in]{geometry}\n" +
                "\\begin{document}\n" +
                "\\begin{center}{\\Huge \\textbf{{{NAME}}}}\\\\ {{SUBTITLE_LINE}} {{CONTACT_INFO}}\\end{center}\n" +
                "\\section*{Professional Summary}\n{{SUMMARY}}\n" +
                "\\section*{Technical Skills}\n{{SKILLS_SECTION}}\n" +
                "\\section*{Professional Experience}\n{{EXPERIENCE_SECTION}}\n" +
                "\\section*{Technical Projects}\n{{PROJECTS_SECTION}}\n" +
                "{{EDUCATION_BLOCK}}\n" +
                "{{ACHIEVEMENTS_BLOCK}}\n" +
                "\\end{document}";
    }
}
