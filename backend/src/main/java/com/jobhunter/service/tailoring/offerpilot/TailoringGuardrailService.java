package com.jobhunter.service.tailoring.offerpilot;

import com.jobhunter.dto.tailoring.offerpilot.SkillClassification;
import com.jobhunter.dto.tailoring.offerpilot.TailoredResumePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Guardrail & Verification Service (Anti-Hallucination Engine).
 * Implements Section 7 of the OfferPilot Architecture Blueprint.
 */
@Service
public class TailoringGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(TailoringGuardrailService.class);

    /**
     * Normalizes a technical skill string by trimming, lowercase, and preserving tech symbols:
     * value.trim().toLowerCase().replaceAll("[^a-z0-9+#. ]", "").replaceAll("\\s+", " ")
     * Correctly preserves "c++", "c#", ".net", "node.js".
     */
    public static String normalizeSkill(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase()
                .replaceAll("[^a-z0-9+#. ]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Runs deterministic guardrail validation against the generated payload:
     * 1. Detects and fails fast if any missing skill leaked into the resume skills section.
     * 2. Injects deterministic audit notes (Missing Skills Disclosure, Missing Required Skills Warning, Fit Warning).
     */
    public void validateAndAugmentNotes(TailoredResumePayload payload, List<String> explicitlyRequiredJdSkills) {
        if (payload == null || payload.getTailoredResume() == null) {
            throw new IllegalArgumentException("Payload or tailoredResume cannot be null");
        }

        TailoredResumePayload.TailoredResumeContent resume = payload.getTailoredResume();
        TailoredResumePayload.SkillsContainer skillsContainer = resume.getSkills();

        // 1. Missing Skill Leak Detection
        Set<String> tailoredSkillsNormalized = new HashSet<>();
        if (skillsContainer != null) {
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getProgrammingLanguages());
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getFrameworks());
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getDatabases());
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getCloud());
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getTools());
            collectNormalized(tailoredSkillsNormalized, skillsContainer.getOther());
        }

        List<SkillClassification> missingSkills = payload.getMissingSkills() != null
                ? payload.getMissingSkills()
                : Collections.emptyList();

        for (SkillClassification missing : missingSkills) {
            if (missing.getSkill() == null || missing.getSkill().isBlank()) continue;
            String normMissing = normalizeSkill(missing.getSkill());
            if (normMissing.isEmpty()) continue;

            if (tailoredSkillsNormalized.contains(normMissing)) {
                log.error("GUARDRAIL_VIOLATION - Missing skill leak detected: [{}] was marked as MISSING but included in tailored resume skills!",
                        missing.getSkill());
                throw new IllegalStateException("Tailored resume contains unsupported missing skill: " + missing.getSkill());
            }
        }

        // 2. Programmatic Note Augmentation
        List<String> auditNotes = new ArrayList<>();

        // a. Missing Skills Disclosure
        if (!missingSkills.isEmpty()) {
            List<String> missingNames = missingSkills.stream()
                    .map(SkillClassification::getSkill)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            if (!missingNames.isEmpty()) {
                auditNotes.add("Some JD skills were not found in the master resume and were not added to the tailored resume: " + missingNames + ".");
            }
        }

        // b. Missing Required Skills Warning
        if (explicitlyRequiredJdSkills != null && !explicitlyRequiredJdSkills.isEmpty()) {
            Set<String> missingNormSet = missingSkills.stream()
                    .map(s -> normalizeSkill(s.getSkill()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            List<String> missingRequired = explicitlyRequiredJdSkills.stream()
                    .filter(req -> missingNormSet.contains(normalizeSkill(req)))
                    .distinct()
                    .collect(Collectors.toList());

            if (!missingRequired.isEmpty()) {
                auditNotes.add("Important: The JD has required skills that were not found in the master resume: "
                        + missingRequired + ". These were not added to avoid unsupported claims.");
            }
        }

        // c. Fit Warning Heuristic
        int s = (payload.getMatchedSkills() != null ? payload.getMatchedSkills().size() : 0)
                + (payload.getPartiallyMatchedSkills() != null ? payload.getPartiallyMatchedSkills().size() : 0);
        int m = missingSkills.size();

        if (m >= 3 && m >= s) {
            auditNotes.add("Fit warning: This role may require several skills not strongly supported by the master resume. Review the missing skills before applying.");
        } else if (m >= 3) {
            auditNotes.add("Review recommended: Multiple JD skills are missing from the master resume. Consider learning or adding verified experience before applying.");
        }

        // Inject notes into both payload.tailoringNotes and resume.tailoringNotes
        for (String note : auditNotes) {
            if (!payload.getTailoringNotes().contains(note)) {
                payload.getTailoringNotes().add(note);
            }
            if (!resume.getTailoringNotes().contains(note)) {
                resume.getTailoringNotes().add(note);
            }
        }
    }

    private void collectNormalized(Set<String> targetSet, List<String> sourceList) {
        if (sourceList == null) return;
        for (String item : sourceList) {
            if (item != null && !item.isBlank()) {
                targetSet.add(normalizeSkill(item));
            }
        }
    }
}
