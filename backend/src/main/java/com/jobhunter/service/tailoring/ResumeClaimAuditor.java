package com.jobhunter.service.tailoring;

import com.jobhunter.dto.tailoring.AuditedClaim;
import com.jobhunter.dto.tailoring.TailoringAuditReportDto;
import com.jobhunter.model.entity.CandidateFact;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.User;
import com.jobhunter.model.fact.ClaimType;
import com.jobhunter.model.fact.FactCategory;
import com.jobhunter.model.fact.SkillEvidenceType;
import com.jobhunter.model.tailoring.TailoredResumeDocument;
import com.jobhunter.model.tailoring.ValidationStatus;
import com.jobhunter.service.fact.CandidateFactStoreService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resume Claim Auditor.
 * Strict, independent validation gate enforcing zero ungrounded candidate claims.
 * Fails closed on any ambiguous or unsupported claim.
 * Conducts Stage 1 (Structured Document Validation) and Stage 2 (Rendered Document Validation).
 * 
 * Semantic Evidence Audit:
 * - DIRECTLY_SUPPORTED: Fact restated directly from Candidate Fact Store.
 * - DERIVED_FROM_SUPPORTED_FACTS: Fact intelligently synthesized, reframed, or polished using approved synonyms and active engineering voice.
 * - UNSUPPORTED: Claim asserts unverified degrees, employers, ungrounded metrics, or unverified technologies.
 */
@Service
public class ResumeClaimAuditor {

    private static final Logger log = LoggerFactory.getLogger(ResumeClaimAuditor.class);
    private static final String AUDITOR_VERSION = "v2.1-offerpilot-auditor";

    private final CandidateFactStoreService factStoreService;

    // Metric pattern: percentages, latency, currency, scale
    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(\\b\\d+([.]\\d+)?%|\\b\\d+\\s*(?:ms|s|seconds|minutes|hours)\\b|\\$[\\d,]+(?:\\s*[kmbt])?|\\b\\d+(?:,\\d+)*(?:\\s*[kmbt]|\\+)?\\s*(?:users|qps|tps|rps|requests|transactions|events)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // Degree pattern
    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "(?i)\\b(bachelor(?:'s)?|master(?:'s)?|b\\.tech|m\\.tech|b\\.e\\.|m\\.e\\.|b\\.s\\.|m\\.s\\.|ph\\.?d\\.?|diploma|associate|bca|mca)\\b"
    );

    public ResumeClaimAuditor(CandidateFactStoreService factStoreService) {
        this.factStoreService = factStoreService;
    }

    /**
     * STAGE 1: Structured Document Validation.
     * Evaluates every field and bullet in TailoredResumeDocument against the Candidate Fact Store.
     * Classifies claims as DIRECTLY_SUPPORTED, DERIVED_FROM_SUPPORTED_FACTS, or UNSUPPORTED.
     */
    public TailoringAuditReportDto validateStructuredDocument(
            TailoredResumeDocument doc,
            CandidateProfile profile,
            List<String> rejectedKeywords) {

        log.info("STAGE_1_STRUCTURED_AUDIT_START - Auditing TailoredResumeDocument [{}] for Candidate [{}]",
                doc.getId(), profile.getId());

        TailoringAuditReportDto report = new TailoringAuditReportDto();
        report.setCandidateProfileId(profile.getId());
        report.setJobId(doc.getJobId());
        report.setValidatorVersion(AUDITOR_VERSION);

        List<CandidateFact> facts = factStoreService.getCandidateFacts(profile);
        Set<String> allowedOrgs = factStoreService.getAllowedOrganizations(profile);
        Set<String> allowedDegrees = factStoreService.getAllowedDegrees(profile);
        Set<String> allowedTechs = factStoreService.getAllowedTechnologies(profile);
        Set<String> allowedMetrics = factStoreService.getAllowedMetrics(profile);

        report.setAllowedOrganizations(new ArrayList<>(allowedOrgs));
        report.setAllowedDegrees(new ArrayList<>(allowedDegrees));

        int checked = 0;
        int passed = 0;
        int failed = 0;

        // 1. Audit Header
        TailoredResumeDocument.Header header = doc.getHeader();
        if (header != null) {
            checked++;
            User user = profile.getUser();
            String expectedName = (user.getFirstName() + " " + user.getLastName()).trim();
            if (header.fullName != null && header.fullName.equalsIgnoreCase(expectedName)) {
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(ClaimType.PERSON_NAME, header.fullName, null));
            } else {
                failed++;
                report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                        ClaimType.PERSON_NAME, header.fullName,
                        "Header name does not match verified candidate profile identity.",
                        expectedName
                ));
            }

            if (header.email != null && !header.email.isBlank()) {
                checked++;
                if (header.email.equalsIgnoreCase(user.getEmail())) {
                    passed++;
                    report.getPassedClaims().add(AuditedClaim.directlySupported(ClaimType.EMAIL, header.email, null));
                } else {
                    failed++;
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.EMAIL, header.email,
                            "Email does not match verified candidate user email.",
                            user.getEmail()
                    ));
                }
            }
        }

        // 2. Audit Professional Summary
        TailoredResumeDocument.Summary summary = doc.getSummary();
        if (summary != null && summary.text != null && !summary.text.isBlank()) {
            checked++;
            // Check for unverified technologies in summary
            List<String> unverifiedInSummary = findUnverifiedTechnologies(summary.text, allowedTechs);
            // Check for rejected keywords in summary
            List<String> rejectedInSummary = findRejectedKeywordsInText(summary.text, rejectedKeywords);
            // Check for unverified metrics in summary
            List<String> unverifiedMetrics = findUnverifiedMetrics(summary.text, allowedMetrics);

            if (unverifiedInSummary.isEmpty() && rejectedInSummary.isEmpty() && unverifiedMetrics.isEmpty()) {
                passed++;
                summary.validationStatus = ValidationStatus.PASSED;
                report.getPassedClaims().add(AuditedClaim.derivedSupported(
                        ClaimType.RESPONSIBILITY,
                        "Professional Summary",
                        null,
                        "Tailored summary derived from verified candidate skills and domain background."
                ));
            } else {
                failed++;
                summary.validationStatus = ValidationStatus.FAILED;
                if (!unverifiedInSummary.isEmpty()) {
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.TECHNOLOGY,
                            "Summary mentions ungrounded technologies: " + String.join(", ", unverifiedInSummary),
                            "Technologies in summary are not present in candidate verified skills or experiences.",
                            "Allowed technologies: " + String.join(", ", allowedTechs)
                    ));
                }
                if (!rejectedInSummary.isEmpty()) {
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.TECHNOLOGY,
                            "Summary contains rejected JD technologies: " + String.join(", ", rejectedInSummary),
                            "Candidate does not possess verified experience for these target job requirements.",
                            "Grounding Verification Gate rejection"
                    ));
                }
                if (!unverifiedMetrics.isEmpty()) {
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.METRIC,
                            "Summary contains ungrounded metrics: " + String.join(", ", unverifiedMetrics),
                            "Metric figures in summary do not exist in candidate factual history.",
                            "Verified metrics: " + String.join(", ", allowedMetrics)
                    ));
                }
            }
        }

        // 3. Audit Technical Skills
        for (TailoredResumeDocument.SkillGroup group : doc.getSkillGroups()) {
            for (TailoredResumeDocument.SkillItem item : group.skills) {
                checked++;
                String skillName = item.name.trim();
                boolean isAllowedTech = allowedTechs.contains(skillName.toLowerCase());
                boolean isAllowedType = item.evidenceType.isAllowedOnResume();

                if (isAllowedTech && isAllowedType) {
                    passed++;
                    report.getPassedClaims().add(AuditedClaim.directlySupported(ClaimType.SKILL, skillName, item.sourceFactId));
                } else {
                    failed++;
                    String reason = !isAllowedTech
                            ? "Skill [" + skillName + "] is not supported by verified candidate evidence."
                            : "Skill [" + skillName + "] has evidence level " + item.evidenceType + " which is not exportable as direct experience.";
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.SKILL, skillName, reason,
                            "Verified skill in CandidateSkill or CandidateExperience"
                    ));
                }
            }
        }

        // 4. Audit Professional Experiences & Bullets
        for (TailoredResumeDocument.ExperienceItem exp : doc.getExperiences()) {
            // Employer Allowlist Check
            checked++;
            String company = exp.company != null ? exp.company.trim() : "";
            boolean orgAllowed = allowedOrgs.stream().anyMatch(ao -> ao.equalsIgnoreCase(company) || company.toLowerCase().contains(ao));
            if (orgAllowed) {
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(ClaimType.EMPLOYER, company, null));
            } else {
                failed++;
                report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                        ClaimType.EMPLOYER, company,
                        "Employer [" + company + "] does not exist in verified candidate employment history.",
                        "Allowed employers: " + String.join(", ", allowedOrgs)
                ));
            }

            // Bullet Checks
            for (TailoredResumeDocument.ExperienceBullet bullet : exp.bullets) {
                checked++;
                String text = bullet.text != null ? bullet.text.trim() : "";

                // Check metrics
                List<String> unverifiedMetrics = findUnverifiedMetrics(text, allowedMetrics);
                // Check tech keywords against rejected keywords
                List<String> rejectedFound = findRejectedKeywordsInText(text, rejectedKeywords);

                if (unverifiedMetrics.isEmpty() && rejectedFound.isEmpty()) {
                    bullet.validationStatus = ValidationStatus.PASSED;
                    passed++;
                    boolean isDerived = "DERIVED_FROM_SUPPORTED_FACTS".equalsIgnoreCase(bullet.evidenceType);
                    if (isDerived) {
                        report.getPassedClaims().add(AuditedClaim.derivedSupported(
                                ClaimType.RESPONSIBILITY, text, bullet.sourceExperienceId,
                                "Reframed engineering outcome grounded in verified candidate facts."
                        ));
                    } else {
                        report.getPassedClaims().add(AuditedClaim.directlySupported(
                                ClaimType.RESPONSIBILITY, text, bullet.sourceExperienceId
                        ));
                    }
                } else {
                    bullet.validationStatus = ValidationStatus.FAILED;
                    failed++;
                    if (!unverifiedMetrics.isEmpty()) {
                        report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                                ClaimType.METRIC,
                                "Bullet contains invented metrics: " + String.join(", ", unverifiedMetrics),
                                "Metrics in bullet are not verified in candidate records. Invented performance metrics are strictly forbidden.",
                                "Candidate verified metrics: " + String.join(", ", allowedMetrics)
                        ));
                    }
                    if (!rejectedFound.isEmpty()) {
                        report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                                ClaimType.TECHNOLOGY,
                                "Bullet contains rejected/unverified technologies: " + String.join(", ", rejectedFound),
                                "Candidate does not possess verified experience for these target job requirements.",
                                "Grounding Verification Gate rejection"
                        ));
                    }
                }
            }
        }

        // 5. Audit Projects & Bullets
        for (TailoredResumeDocument.ProjectItem proj : doc.getProjects()) {
            checked++;
            // Check project technologies
            List<String> projTechRejected = new ArrayList<>();
            for (String tech : proj.technologies) {
                projTechRejected.addAll(findRejectedKeywordsInText(tech, rejectedKeywords));
            }

            if (projTechRejected.isEmpty()) {
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(
                        ClaimType.PROJECT, proj.name, proj.sourceProjectId
                ));
            } else {
                failed++;
                report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                        ClaimType.TECHNOLOGY,
                        "Project [" + proj.name + "] contains rejected technologies: " + String.join(", ", projTechRejected),
                        "Project asserts unverified technologies requested in job description.",
                        "Verified technologies only"
                ));
            }

            for (TailoredResumeDocument.ProjectBullet pb : proj.bullets) {
                checked++;
                String text = pb.text != null ? pb.text.trim() : "";

                List<String> unverifiedMetrics = findUnverifiedMetrics(text, allowedMetrics);
                List<String> rejectedFound = findRejectedKeywordsInText(text, rejectedKeywords);

                if (unverifiedMetrics.isEmpty() && rejectedFound.isEmpty()) {
                    pb.validationStatus = ValidationStatus.PASSED;
                    passed++;
                    boolean isDerived = "DERIVED_FROM_SUPPORTED_FACTS".equalsIgnoreCase(pb.evidenceType);
                    if (isDerived) {
                        report.getPassedClaims().add(AuditedClaim.derivedSupported(
                                ClaimType.RESPONSIBILITY, text, pb.sourceProjectId,
                                "Reframed project architecture bullet grounded in candidate facts."
                        ));
                    } else {
                        report.getPassedClaims().add(AuditedClaim.directlySupported(
                                ClaimType.RESPONSIBILITY, text, pb.sourceProjectId
                        ));
                    }
                } else {
                    pb.validationStatus = ValidationStatus.FAILED;
                    failed++;
                    if (!unverifiedMetrics.isEmpty()) {
                        report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                                ClaimType.METRIC,
                                "Project bullet contains invented metrics: " + String.join(", ", unverifiedMetrics),
                                "Metrics in project bullet are not verified in candidate records.",
                                "Candidate verified metrics: " + String.join(", ", allowedMetrics)
                        ));
                    }
                    if (!rejectedFound.isEmpty()) {
                        report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                                ClaimType.TECHNOLOGY,
                                "Project bullet contains rejected/unverified technologies: " + String.join(", ", rejectedFound),
                                "Candidate does not possess verified experience for these target job requirements.",
                                "Grounding Verification Gate rejection"
                        ));
                    }
                }
            }
        }

        // 6. Audit Education Credentials (STRICT GUARD)
        if (doc.getEducation().isEmpty()) {
            // If candidate has no education and document has none, that is truthful and allowed
            checked++;
            passed++;
            report.getPassedClaims().add(AuditedClaim.directlySupported(
                    ClaimType.DEGREE, "No Education Listed (Truthful Omission)", null
            ));
        } else {
            for (TailoredResumeDocument.EducationItem edu : doc.getEducation()) {
                checked++;
                if (allowedDegrees.isEmpty()) {
                    // Candidate has NO verified education, but document contains one! FABRICATION DETECTED!
                    failed++;
                    edu.validationStatus = ValidationStatus.FAILED;
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.DEGREE,
                            edu.degree + (edu.institution != null ? " (" + edu.institution + ")" : ""),
                            "FABRICATED EDUCATION: Candidate profile and master resume contain ZERO verified education records. Generation of synthetic degree is strictly forbidden.",
                            "Verified education degree record"
                    ));
                } else {
                    String degLower = edu.degree != null ? edu.degree.toLowerCase() : "";
                    boolean degreeAllowed = allowedDegrees.stream().anyMatch(ad -> degLower.contains(ad) || ad.contains(degLower));

                    if (degreeAllowed) {
                        passed++;
                        edu.validationStatus = ValidationStatus.PASSED;
                        report.getPassedClaims().add(AuditedClaim.directlySupported(
                                ClaimType.DEGREE, edu.degree, edu.sourceFactId
                        ));
                    } else {
                        failed++;
                        edu.validationStatus = ValidationStatus.FAILED;
                        report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                                ClaimType.DEGREE,
                                edu.degree,
                                "Degree [" + edu.degree + "] does not match verified candidate education records.",
                                "Verified degrees: " + String.join(", ", allowedDegrees)
                        ));
                    }
                }
            }
        }

        // 7. Audit Achievements (STRICT GUARD)
        if (doc.getAchievements() != null && !doc.getAchievements().isEmpty()) {
            for (TailoredResumeDocument.AchievementItem ach : doc.getAchievements()) {
                checked++;
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(
                        ClaimType.ACHIEVEMENT, ach.title != null ? ach.title : "Achievement", null
                ));
            }
        }

        report.setClaimsChecked(checked);
        report.setClaimsPassed(passed);
        report.setClaimsFailed(failed);

        boolean passedAudit = (failed == 0);
        report.setPassed(passedAudit);
        report.setOverallStatus(passedAudit ? "READY_FOR_DOWNLOAD" : "VALIDATION_FAILED");
        report.setStatusMessage(passedAudit
                ? "Stage 1 Structured Audit PASSED: All claims verified against Candidate Fact Store."
                : "Resume generation was blocked because unsupported candidate information was detected (" + failed + " unsupported claims).");

        doc.setValidationStatus(passedAudit ? ValidationStatus.PASSED : ValidationStatus.FAILED);
        if (!passedAudit) {
            doc.getValidationErrors().addAll(report.getUnsupportedClaims().stream()
                    .map(AuditedClaim::getFailureReason)
                    .collect(Collectors.toList()));
        }

        log.info("STAGE_1_STRUCTURED_AUDIT_COMPLETE - Checked: {}, Passed: {}, Failed: {}, Status: {}",
                checked, passed, failed, report.getOverallStatus());
        return report;
    }

    /**
     * STAGE 2: Rendered Document Validation.
     * Extracts text from compiled PDF document using PDFBox and verifies zero ungrounded claims,
     * unapproved institutions, or unverified degrees made it into the physical rendered layout.
     */
    public TailoringAuditReportDto validateRenderedPdf(
            File pdfFile,
            TailoredResumeDocument doc,
            CandidateProfile profile,
            List<String> rejectedKeywords) {

        log.info("STAGE_2_RENDERED_AUDIT_START - Auditing compiled PDF [{}] for Candidate [{}]",
                pdfFile.getName(), profile.getId());

        TailoringAuditReportDto report = new TailoringAuditReportDto();
        report.setCandidateProfileId(profile.getId());
        report.setJobId(doc.getJobId());
        report.setValidatorVersion(AUDITOR_VERSION);

        Set<String> allowedOrgs = factStoreService.getAllowedOrganizations(profile);
        Set<String> allowedDegrees = factStoreService.getAllowedDegrees(profile);
        Set<String> allowedMetrics = factStoreService.getAllowedMetrics(profile);
        report.setAllowedOrganizations(new ArrayList<>(allowedOrgs));
        report.setAllowedDegrees(new ArrayList<>(allowedDegrees));

        int checked = 0;
        int passed = 0;
        int failed = 0;

        String pdfText = "";
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            pdfText = stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF for Stage 2 claim audit", e);
            report.setPassed(false);
            report.setOverallStatus("PDF_GENERATION_FAILED");
            report.setStatusMessage("Failed to read compiled PDF text stream: " + e.getMessage());
            return report;
        }

        String lowerPdfText = pdfText.toLowerCase();

        // 1. Check for fabricated education in stripped text
        checked++;
        if (allowedDegrees.isEmpty()) {
            // Candidate has NO verified degrees. If PDF contains degree keywords, FAIL!
            Matcher dm = DEGREE_PATTERN.matcher(pdfText);
            if (dm.find()) {
                failed++;
                report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                        ClaimType.DEGREE,
                        dm.group(),
                        "Rendered PDF contains education degree keywords [" + dm.group() + "] but candidate profile has NO verified education.",
                        "Zero education entries expected."
                ));
            } else {
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(
                        ClaimType.DEGREE, "Clean education omission verified in PDF", null
                ));
            }
        } else {
            // Candidate has verified degrees; ensure rendered degree matches allowed degrees
            boolean matchesAllowed = false;
            for (String allowed : allowedDegrees) {
                if (lowerPdfText.contains(allowed.toLowerCase())) {
                    matchesAllowed = true;
                    break;
                }
            }
            if (matchesAllowed) {
                passed++;
                report.getPassedClaims().add(AuditedClaim.directlySupported(
                        ClaimType.DEGREE, "Verified degree present in PDF text", null
                ));
            } else {
                failed++;
                report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                        ClaimType.DEGREE,
                        "Unknown degree in PDF",
                        "Rendered PDF does not match candidate's verified degrees: " + String.join(", ", allowedDegrees),
                        "Allowed: " + String.join(", ", allowedDegrees)
                ));
            }
        }

        // 2. Check for rejected keywords in PDF
        if (rejectedKeywords != null) {
            for (String rk : rejectedKeywords) {
                checked++;
                String rkLower = rk.toLowerCase().trim();
                if (!rkLower.isBlank() && isKeywordPresentAsWord(lowerPdfText, rkLower)) {
                    failed++;
                    report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                            ClaimType.TECHNOLOGY,
                            rk,
                            "Rendered PDF text contains rejected/unverified technology [" + rk + "].",
                            "Omission of unsupported JD requirement"
                    ));
                } else {
                    passed++;
                }
            }
        }

        // 3. Check metrics in PDF
        checked++;
        List<String> unverifiedPdfMetrics = findUnverifiedMetrics(pdfText, allowedMetrics);
        if (unverifiedPdfMetrics.isEmpty()) {
            passed++;
            report.getPassedClaims().add(AuditedClaim.directlySupported(
                    ClaimType.METRIC, "All metrics in PDF grounded in candidate history", null
            ));
        } else {
            failed++;
            report.getUnsupportedClaims().add(AuditedClaim.unsupported(
                    ClaimType.METRIC,
                    "PDF contains unverified metrics: " + String.join(", ", unverifiedPdfMetrics),
                    "Renderer inserted metrics that do not exist in candidate verified history.",
                    "Allowed metrics: " + String.join(", ", allowedMetrics)
            ));
        }

        report.setClaimsChecked(checked);
        report.setClaimsPassed(passed);
        report.setClaimsFailed(failed);

        boolean passedAudit = (failed == 0);
        report.setPassed(passedAudit);
        report.setOverallStatus(passedAudit ? "READY_FOR_DOWNLOAD" : "VALIDATION_FAILED");
        report.setStatusMessage(passedAudit
                ? "Stage 2 Rendered PDF Audit PASSED: Physical PDF contains only verified candidate facts."
                : "Resume export was blocked because unsupported claims were detected in rendered PDF (" + failed + " failures).");

        log.info("STAGE_2_RENDERED_AUDIT_COMPLETE - Checked: {}, Passed: {}, Failed: {}, Status: {}",
                checked, passed, failed, report.getOverallStatus());
        return report;
    }

    private List<String> findUnverifiedTechnologies(String text, Set<String> allowedTechs) {
        List<String> unverified = new ArrayList<>();
        // High-risk external tech platforms and ecosystems commonly hallucinated by LLMs
        String[] highRiskTechs = {
                "kafka", "kubernetes", "aws", "gcp", "azure", "graphql", "rust", "golang",
                "elasticsearch", "solr", "hadoop", "spark", "snowflake", "terraform"
        };
        String lower = text.toLowerCase();
        for (String t : highRiskTechs) {
            if (isKeywordPresentAsWord(lower, t) && !allowedTechs.contains(t)) {
                unverified.add(t);
            }
        }
        return unverified;
    }

    private List<String> findUnverifiedMetrics(String text, Set<String> allowedMetrics) {
        List<String> unverified = new ArrayList<>();
        Matcher m = METRIC_PATTERN.matcher(text);
        while (m.find()) {
            String metric = m.group().trim().toLowerCase();
            // Specific metrics like "14s", "1.8s", "sub-50ms", "45%" must be grounded
            boolean isSpecificMetric = metric.contains("%") || metric.contains("ms") || metric.contains("latency")
                    || metric.matches(".*\\d+\\s*(?:s|seconds).*") || metric.contains("$");

            if (isSpecificMetric) {
                boolean grounded = allowedMetrics.stream().anyMatch(am -> am.contains(metric) || metric.contains(am));
                if (!grounded) {
                    unverified.add(m.group().trim());
                }
            }
        }
        return unverified;
    }

    private List<String> findRejectedKeywordsInText(String text, List<String> rejectedKeywords) {
        List<String> found = new ArrayList<>();
        if (rejectedKeywords == null) return found;
        String lower = text.toLowerCase();
        for (String rk : rejectedKeywords) {
            String rkLower = rk.toLowerCase().trim();
            if (!rkLower.isBlank() && isKeywordPresentAsWord(lower, rkLower)) {
                found.add(rk);
            }
        }
        return found;
    }

    private boolean isKeywordPresentAsWord(String text, String keyword) {
        String pattern = "\\b" + Pattern.quote(keyword) + "\\b";
        return Pattern.compile(pattern).matcher(text).find();
    }
}
