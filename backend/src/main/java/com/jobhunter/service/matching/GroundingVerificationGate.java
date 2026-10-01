package com.jobhunter.service.matching;

import com.jobhunter.model.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GroundingVerificationGate {

    private static final Logger log = LoggerFactory.getLogger(GroundingVerificationGate.class);

    private static final Set<String> COMMON_NON_TECH_WORDS = Set.of(
            "the", "and", "for", "with", "years", "experience", "building", "strong", "team", "role", "work",
            "must", "have", "preferred", "bachelor", "degree", "science", "engineering", "seniority", "location",
            "proficiency", "hands", "hands-on", "knowledge", "required", "familiarity", "understanding",
            "deep", "excellent", "working", "using", "practices", "solid", "track", "record",
            "in", "on", "at", "to", "of", "is", "as", "by", "an", "or", "from"
    );

    public record GroundingResult(
            boolean passed,
            String validatedClaim,
            String rejectedReason,
            boolean convertedToGap
    ) {}

    /**
     * Verifies that a technical claim is strictly grounded in candidate skills, experiences, or projects.
     * If ungrounded, returns passed=false and converts the item to a GAP.
     */
    public GroundingResult verifyTechnicalClaim(String requirement, String candidateClaim, CandidateProfile profile) {
        if (candidateClaim == null || candidateClaim.isBlank()) {
            return new GroundingResult(false, null, "Candidate evidence is completely empty", true);
        }

        Set<String> verifiedTech = extractAllCandidateTechnologies(profile);
        String lowerClaim = candidateClaim.toLowerCase();

        // Extract technical tokens mentioned in the requirement
        List<String> requiredTechs = extractPotentialTechWords(requirement);

        for (String reqTech : requiredTechs) {
            String lowerReq = reqTech.toLowerCase();

            // Check if claim asserts candidate has this specific tech
            boolean assertsTech = lowerClaim.contains(lowerReq);
            if (assertsTech) {
                // Verify whether candidate ACTUALLY has this tech in verified profile
                boolean candidateHasIt = verifiedTech.contains(lowerReq) || hasFuzzyTechMatch(verifiedTech, lowerReq);
                if (!candidateHasIt) {
                    log.warn("GROUNDING_GATE_REJECTED - Untruthful claim detected: Candidate claims [{}] but profile has NO evidence for [{}]", candidateClaim, reqTech);
                    return new GroundingResult(
                            false,
                            "No verified candidate evidence found for " + reqTech + ".",
                            "Claimed technology [" + reqTech + "] does not exist in candidate verified skills, experiences, or projects.",
                            true // CONVERT TO GAP
                    );
                }
            }
        }

        // Verify commercial vs project claim
        boolean claimsCommercial = lowerClaim.contains("commercial") || lowerClaim.contains("production");
        if (claimsCommercial) {
            for (String reqTech : requiredTechs) {
                String lowerReq = reqTech.toLowerCase();
                if (lowerClaim.contains(lowerReq)) {
                    boolean isCommercial = isCommercialSkill(profile, lowerReq);
                    if (!isCommercial) {
                        String corrected = "Project/Academic experience with " + reqTech + " (No commercial production track record).";
                        log.info("GROUNDING_GATE_ADJUSTED - Demoted [{}] from commercial to project-only experience", reqTech);
                        return new GroundingResult(true, corrected, null, false);
                    }
                }
            }
        }

        return new GroundingResult(true, candidateClaim, null, false);
    }

    public Set<String> extractAllCandidateTechnologies(CandidateProfile profile) {
        Set<String> tech = new HashSet<>();

        if (profile.getSkills() != null) {
            for (CandidateSkill s : profile.getSkills()) {
                if (s.getSkill() != null) {
                    tech.add(s.getSkill().getName().toLowerCase());
                }
            }
        }

        if (profile.getExperiences() != null) {
            for (CandidateExperience exp : profile.getExperiences()) {
                tech.addAll(extractTechFromJsonString(exp.getTechnologies()));
                scanAndAddTechTerms(exp.getResponsibilities(), tech);
                scanAndAddTechTerms(exp.getEvidenceText(), tech);
                scanAndAddTechTerms(exp.getAchievements(), tech);
            }
        }

        if (profile.getProjects() != null) {
            for (CandidateProject proj : profile.getProjects()) {
                tech.addAll(extractTechFromJsonString(proj.getTechnologies()));
                scanAndAddTechTerms(proj.getDescription(), tech);
                scanAndAddTechTerms(proj.getEvidenceText(), tech);
                scanAndAddTechTerms(proj.getArchitecture(), tech);
            }
        }

        return tech;
    }

    private void scanAndAddTechTerms(String text, Set<String> tech) {
        if (text == null || text.isBlank()) return;
        String lower = text.toLowerCase();
        String[] keywords = {
                "microservices", "microservice", "rest apis", "rest", "api", "jwt",
                "spring security", "docker", "git", "sql", "postgresql", "react",
                "node.js", "redis", "websocket", "testcontainers", "swagger", "openapi",
                "b-tree", "acid", "idempotency"
        };
        for (String kw : keywords) {
            if (lower.contains(kw)) {
                tech.add(kw);
            }
        }
    }

    public boolean isCommercialSkill(CandidateProfile profile, String techLower) {
        if (profile.getSkills() != null) {
            for (CandidateSkill s : profile.getSkills()) {
                if (s.getSkill() != null) {
                    String name = s.getSkill().getName().toLowerCase();
                    if (name.equalsIgnoreCase(techLower)) {
                        return "COMMERCIAL".equalsIgnoreCase(s.getExperienceType());
                    }
                    if (techLower.equals("spring") && name.equals("spring boot")) {
                        return "COMMERCIAL".equalsIgnoreCase(s.getExperienceType());
                    }
                    if ((techLower.equals("microservices") || techLower.equals("microservice")) && (name.equals("spring boot") || name.equals("rest apis"))) {
                        return "COMMERCIAL".equalsIgnoreCase(s.getExperienceType());
                    }
                }
            }
        }
        if (profile.getExperiences() != null) {
            for (CandidateExperience exp : profile.getExperiences()) {
                if (extractTechFromJsonString(exp.getTechnologies()).contains(techLower)) {
                    return true;
                }
                String resp = exp.getResponsibilities() != null ? exp.getResponsibilities().toLowerCase() : "";
                String ev = exp.getEvidenceText() != null ? exp.getEvidenceText().toLowerCase() : "";
                if (resp.contains(techLower) || ev.contains(techLower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasFuzzyTechMatch(Set<String> verifiedTech, String target) {
        for (String v : verifiedTech) {
            if (v.equalsIgnoreCase(target) || v.contains(target) || target.contains(v)) {
                return true;
            }
            if ((target.equals("microservices") || target.equals("microservice")) && (v.contains("microservice") || v.contains("spring boot") || v.contains("rest api"))) {
                return true;
            }
            if ((target.equals("spring") || target.equals("spring framework")) && v.contains("spring boot")) {
                return true;
            }
            if ((target.equals("database") || target.equals("rdbms") || target.equals("relational")) && (v.contains("postgresql") || v.contains("sql"))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractTechFromJsonString(String json) {
        Set<String> set = new HashSet<>();
        if (json == null || json.isBlank()) return set;
        String clean = json.replaceAll("[\\[\\]\"']", "");
        for (String part : clean.split(",")) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isBlank()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private List<String> extractPotentialTechWords(String requirement) {
        List<String> list = new ArrayList<>();
        // Match words starting with letters/digits that may contain +, #, ., -
        Pattern p = Pattern.compile("\\b([A-Za-z0-9\\+\\#\\.\\-]+)\\b");
        Matcher m = p.matcher(requirement);
        while (m.find()) {
            String word = m.group(1);
            if (!COMMON_NON_TECH_WORDS.contains(word.toLowerCase()) && word.length() > 1) {
                list.add(word);
            }
        }
        return list;
    }
}
