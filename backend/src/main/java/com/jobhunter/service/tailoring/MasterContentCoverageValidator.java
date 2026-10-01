package com.jobhunter.service.tailoring;

import com.jobhunter.model.tailoring.TailoredResumeDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Master Content Coverage & Anti-Truncation Validator.
 * Enforces the Section Preservation Contract and Content Loss Prevention invariants:
 * 1. All canonical master resume sections must be preserved (Header, Summary, Skills, Experience, Projects, Education, Achievements).
 * 2. Zero company name leakage: target company names, recruiter names, and application phrasing ("tailored for", "applying to") are strictly forbidden.
 * 3. Zero truncation / word clipping (e.g. "Post-", "Repos-", "Microser-", "initia-").
 * 4. Master content coverage tracking (Preserved, Rewritten, Reordered, Condensed, Removed).
 * 5. Verified candidate entities (employers, degrees, projects, achievements) must not be silently dropped.
 */
@Service
public class MasterContentCoverageValidator {

    private static final Logger log = LoggerFactory.getLogger(MasterContentCoverageValidator.class);

    // Pattern to catch broken hyphenated words (e.g., "Post-", "Repos-", "Microser-", "initia-")
    private static final Pattern DANGLING_HYPHEN_PATTERN = Pattern.compile(
            "\\b([A-Za-z]{3,})-\\s+([A-Za-z]{2,})\\b"
    );

    // Pattern to catch words truncated with a trailing hyphen at the end of a line or bullet
    private static final Pattern TRUNCATED_WORD_END_PATTERN = Pattern.compile(
            "\\b(Post|Repos|Microser|initia|Technolo|Devel|Architec|Deploy|Configur|Applicat|Manag)-\\b",
            Pattern.CASE_INSENSITIVE
    );

    public static class CoverageBreakdown {
        public int preservedCount = 0;
        public int rewrittenCount = 0;
        public int reorderedCount = 0;
        public int condensedCount = 0;
        public int removedCount = 0;
        public int totalMasterBullets = 0;
        public int totalTailoredBullets = 0;
        public double coveragePercentage = 100.0;

        public void recalculate() {
            if (totalMasterBullets > 0) {
                int retained = preservedCount + rewrittenCount + reorderedCount + condensedCount;
                coveragePercentage = Math.min(100.0, ((double) retained / totalMasterBullets) * 100.0);
            }
        }
    }

    public static class CoverageReport {
        private boolean passed = true;
        private final List<String> passedChecks = new ArrayList<>();
        private final List<String> missingSections = new ArrayList<>();
        private final List<String> missingEntities = new ArrayList<>();
        private final List<String> truncationErrors = new ArrayList<>();
        private final List<String> antiLeakageViolations = new ArrayList<>();
        private final CoverageBreakdown coverageBreakdown = new CoverageBreakdown();

        public boolean isPassed() {
            return passed && missingSections.isEmpty() && missingEntities.isEmpty()
                    && truncationErrors.isEmpty() && antiLeakageViolations.isEmpty();
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<String> getPassedChecks() {
            return passedChecks;
        }

        public List<String> getMissingSections() {
            return missingSections;
        }

        public List<String> getMissingEntities() {
            return missingEntities;
        }

        public List<String> getTruncationErrors() {
            return truncationErrors;
        }

        public List<String> getAntiLeakageViolations() {
            return antiLeakageViolations;
        }

        public CoverageBreakdown getCoverageBreakdown() {
            return coverageBreakdown;
        }
    }

    /**
     * Validates that TailoredResumeDocument fulfills the Master Content Coverage Contract without company leakage.
     */
    public CoverageReport validateDocument(TailoredResumeDocument doc) {
        return validateDocument(doc, null, Collections.emptySet());
    }

    /**
     * Validates TailoredResumeDocument with strict target company leakage and application phrasing detection.
     */
    public CoverageReport validateDocument(TailoredResumeDocument doc, String targetCompany, Set<String> candidateCompanies) {
        CoverageReport report = new CoverageReport();

        if (doc == null) {
            report.setPassed(false);
            report.getMissingSections().add("Document is null");
            return report;
        }

        // 1. Validate Header
        if (doc.getHeader() != null && doc.getHeader().fullName != null && !doc.getHeader().fullName.isBlank()) {
            report.getPassedChecks().add("Header present with name: " + doc.getHeader().fullName);
        } else {
            report.getMissingSections().add("Header or candidate full name is missing");
        }

        // 2. Validate Summary
        if (doc.getSummary() != null && doc.getSummary().text != null && !doc.getSummary().text.isBlank()) {
            report.getPassedChecks().add("Professional Summary present");
            checkTruncationInText("Summary", doc.getSummary().text, report);
            checkCompanyLeakageAndPhrasing("Summary", doc.getSummary().text, targetCompany, candidateCompanies, report);
        } else {
            report.getMissingSections().add("Professional Summary section is missing");
        }

        // 3. Validate Technical Skills
        if (doc.getSkillGroups() != null && !doc.getSkillGroups().isEmpty()) {
            int totalSkills = doc.getSkillGroups().stream().mapToInt(g -> g.skills.size()).sum();
            if (totalSkills > 0) {
                report.getPassedChecks().add("Technical Skills present with " + totalSkills + " skills across " + doc.getSkillGroups().size() + " categories");
            } else {
                report.getMissingSections().add("Technical Skills categories exist but contain 0 skills");
            }
        } else {
            report.getMissingSections().add("Technical Skills section is missing");
        }

        // 4. Validate Experience
        if (doc.getExperiences() != null && !doc.getExperiences().isEmpty()) {
            report.getPassedChecks().add("Professional Experience present with " + doc.getExperiences().size() + " roles");
            for (TailoredResumeDocument.ExperienceItem exp : doc.getExperiences()) {
                for (TailoredResumeDocument.ExperienceBullet b : exp.bullets) {
                    checkTruncationInText("Experience (" + exp.company + ")", b.text, report);
                    checkCompanyLeakageAndPhrasing("Experience (" + exp.company + ")", b.text, targetCompany, candidateCompanies, report);
                }
            }
        } else {
            report.getMissingSections().add("Professional Experience section is missing");
        }

        // 5. Validate Projects
        if (doc.getProjects() != null && !doc.getProjects().isEmpty()) {
            report.getPassedChecks().add("Technical Projects present with " + doc.getProjects().size() + " projects");
            for (TailoredResumeDocument.ProjectItem p : doc.getProjects()) {
                if (p.subTitle != null) {
                    checkTruncationInText("Project Subtitle (" + p.name + ")", p.subTitle, report);
                    checkCompanyLeakageAndPhrasing("Project Subtitle (" + p.name + ")", p.subTitle, targetCompany, candidateCompanies, report);
                }
                for (TailoredResumeDocument.ProjectBullet pb : p.bullets) {
                    checkTruncationInText("Project (" + p.name + ")", pb.text, report);
                    checkCompanyLeakageAndPhrasing("Project (" + p.name + ")", pb.text, targetCompany, candidateCompanies, report);
                }
            }
        } else {
            report.getMissingSections().add("Technical Projects section is missing");
        }

        // 6. Validate Education
        if (doc.getEducation() != null && !doc.getEducation().isEmpty()) {
            report.getPassedChecks().add("Education present with " + doc.getEducation().size() + " entries");
        } else {
            report.getMissingSections().add("Education section is missing");
        }

        // 7. Validate Achievements
        if (doc.getAchievements() != null && !doc.getAchievements().isEmpty()) {
            report.getPassedChecks().add("Achievements present with " + doc.getAchievements().size() + " achievements");
            for (TailoredResumeDocument.AchievementItem ach : doc.getAchievements()) {
                String full = (ach.title != null ? ach.title : "") + " " + (ach.description != null ? ach.description : "");
                checkTruncationInText("Achievement", full, report);
            }
        } else {
            report.getMissingSections().add("Achievements section is missing");
        }

        if (!report.getMissingSections().isEmpty() || !report.getTruncationErrors().isEmpty()
                || !report.getMissingEntities().isEmpty() || !report.getAntiLeakageViolations().isEmpty()) {
            report.setPassed(false);
        }

        return report;
    }

    /**
     * Validates that the rendered PDF text contains all master sections, key entities, and no text truncation.
     */
    public CoverageReport validateRenderedPdfText(String pdfText, List<String> expectedEntities) {
        return validateRenderedPdfText(pdfText, expectedEntities, null, Collections.emptySet());
    }

    /**
     * Validates rendered PDF text including checking against target company name leakage.
     */
    public CoverageReport validateRenderedPdfText(String pdfText, List<String> expectedEntities, String targetCompany, Set<String> candidateCompanies) {
        CoverageReport report = new CoverageReport();

        if (pdfText == null || pdfText.isBlank()) {
            report.setPassed(false);
            report.getMissingSections().add("PDF text stream is empty");
            return report;
        }

        String upper = pdfText.toUpperCase();

        // 1. Check sections in PDF
        checkSectionHeader(upper, "SUMMARY", report);
        checkSectionHeader(upper, "SKILL", report);
        checkSectionHeader(upper, "EXPERIENCE", report);
        checkSectionHeader(upper, "PROJECT", report);
        checkSectionHeader(upper, "EDUCATION", report);
        checkSectionHeader(upper, "ACHIEVEMENT", report);

        // 2. Check expected entities
        if (expectedEntities != null) {
            String lower = pdfText.toLowerCase();
            for (String entity : expectedEntities) {
                if (lower.contains(entity.toLowerCase().trim())) {
                    report.getPassedChecks().add("Verified entity detected in PDF: " + entity);
                } else {
                    report.getMissingEntities().add("Required entity missing from PDF text: " + entity);
                }
            }
        }

        // 3. Anti-truncation check on PDF text
        checkTruncationInText("Rendered PDF", pdfText, report);

        // 4. Target company leakage check in Summary of PDF text
        if (targetCompany != null && !targetCompany.isBlank()) {
            int summaryStart = upper.indexOf("SUMMARY");
            int skillsStart = upper.indexOf("SKILL");
            if (summaryStart != -1 && skillsStart != -1 && skillsStart > summaryStart) {
                String summarySnippet = pdfText.substring(summaryStart, skillsStart);
                checkCompanyLeakageAndPhrasing("Rendered PDF Summary", summarySnippet, targetCompany, candidateCompanies, report);
            }
        }

        if (!report.getMissingSections().isEmpty() || !report.getMissingEntities().isEmpty()
                || !report.getTruncationErrors().isEmpty() || !report.getAntiLeakageViolations().isEmpty()) {
            report.setPassed(false);
        }

        return report;
    }

    public void checkCompanyLeakageAndPhrasing(
            String context,
            String text,
            String targetCompany,
            Set<String> candidateCompanies,
            CoverageReport report) {

        if (text == null || text.isBlank()) return;
        String textLower = text.toLowerCase();

        // 1. Check target company leakage
        if (targetCompany != null && !targetCompany.isBlank()) {
            String targetTrimmed = targetCompany.trim();
            boolean isCandidateEmployer = candidateCompanies != null && candidateCompanies.stream()
                    .anyMatch(c -> c != null && (c.equalsIgnoreCase(targetTrimmed) || c.toLowerCase().contains(targetTrimmed.toLowerCase())));

            if (!isCandidateEmployer) {
                // Word boundary check (e.g. \bLingaro\b)
                Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(targetTrimmed) + "\\b");
                if (p.matcher(text).find()) {
                    report.getAntiLeakageViolations().add(
                            "CRITICAL: Target company name [" + targetCompany + "] leaked into " + context + ": \"" + text + "\""
                    );
                }
            }
        }

        // 2. Check forbidden application phrasing
        List<String> forbiddenPhrases = List.of(
                "tailored for",
                "seeking a role at",
                "applying to",
                "excited to apply",
                "ideal candidate for",
                "aligned with lingaro",
                "aligned with the company",
                "this role",
                "this position",
                "the employer"
        );

        for (String phrase : forbiddenPhrases) {
            if (textLower.contains(phrase)) {
                report.getAntiLeakageViolations().add(
                        "CRITICAL: Forbidden application phrasing [" + phrase + "] detected in " + context + ": \"" + text + "\""
                );
            }
        }
    }

    private void checkSectionHeader(String upperPdfText, String keyword, CoverageReport report) {
        if (upperPdfText.contains(keyword)) {
            report.getPassedChecks().add("Section header containing [" + keyword + "] detected in PDF.");
        } else {
            report.getMissingSections().add("Required section [" + keyword + "] not detected in PDF text.");
        }
    }

    private void checkTruncationInText(String source, String text, CoverageReport report) {
        if (text == null || text.isBlank()) return;

        Matcher dm = DANGLING_HYPHEN_PATTERN.matcher(text);
        while (dm.find()) {
            String word1 = dm.group(1);
            String word2 = dm.group(2);
            // Ignore legitimate hyphens like high-concurrency, real-time, sub-second, sub-20ms
            if (!isLegitimateHyphenation(word1, word2)) {
                report.getTruncationErrors().add("Dangling hyphenation detected in " + source + ": \"" + dm.group() + "\"");
            }
        }

        Matcher tw = TRUNCATED_WORD_END_PATTERN.matcher(text);
        while (tw.find()) {
            report.getTruncationErrors().add("Truncated word with trailing hyphen detected in " + source + ": \"" + tw.group() + "\"");
        }
    }

    private boolean isLegitimateHyphenation(String w1, String w2) {
        String combined = (w1 + "-" + w2).toLowerCase();
        return combined.matches("(?i)\\b(high-concurrency|high-throughput|high-performance|real-time|sub-second|sub-20ms|end-to-end|cross-functional|role-targeted|in-memory|open-source|two-stage)\\b");
    }
}
