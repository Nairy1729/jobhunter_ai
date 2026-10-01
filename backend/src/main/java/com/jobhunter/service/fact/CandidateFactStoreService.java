package com.jobhunter.service.fact;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.*;
import com.jobhunter.model.fact.EvidenceLevel;
import com.jobhunter.model.fact.FactCategory;
import com.jobhunter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Canonical Candidate Fact Store Service.
 * Invariant: The model is NOT the source of truth for candidate facts.
 * Verified candidate evidence is the SOLE source of truth.
 * Job requirements NEVER become candidate evidence.
 */
@Service
public class CandidateFactStoreService {

    private static final Logger log = LoggerFactory.getLogger(CandidateFactStoreService.class);

    private final CandidateFactRepository candidateFactRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateProjectRepository projectRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    // Pattern to capture explicit metrics: percentages, latency, throughput, scale numbers, currency
    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "(\\b\\d+([.]\\d+)?%|\\b\\d+\\s*(?:ms|s|seconds|minutes|hours)\\b|\\$[\\d,]+(?:\\s*[kmbt])?|\\b\\d+(?:,\\d+)*(?:\\s*[kmbt]|\\+)?\\s*(?:users|qps|tps|rps|requests|transactions|events)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // Degree keywords for education parsing
    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "(?i)\\b(bachelor(?:'s)?|master(?:'s)?|b\\.tech|m\\.tech|b\\.e\\.|m\\.e\\.|b\\.s\\.|m\\.s\\.|ph\\.?d\\.?|diploma|associate|bca|mca)\\b.*?(?:in\\s+[a-zA-Z\\s&]+)?"
    );

    public CandidateFactStoreService(
            CandidateFactRepository candidateFactRepository,
            CandidateExperienceRepository experienceRepository,
            CandidateProjectRepository projectRepository,
            CandidateSkillRepository candidateSkillRepository,
            ResumeRepository resumeRepository,
            ObjectMapper objectMapper) {
        this.candidateFactRepository = candidateFactRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts and synchronizes the canonical fact store for a CandidateProfile.
     * Guaranteed to include only facts grounded in profile, resume, experiences, projects, and skills.
     */
    @Transactional
    public List<CandidateFact> syncCandidateFacts(CandidateProfile profile) {
        log.info("CANDIDATE_FACT_SYNC_START - Synchronizing canonical facts for Candidate [{}]", profile.getId());

        List<CandidateFact> facts = new ArrayList<>();
        User user = profile.getUser();

        // 1. Candidate Identity
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
        facts.add(new CandidateFact(profile, FactCategory.OTHER, fullName, "USER_PROFILE", "fullName", EvidenceLevel.VERIFIED, true));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            facts.add(new CandidateFact(profile, FactCategory.OTHER, user.getEmail().trim(), "USER_PROFILE", "email", EvidenceLevel.VERIFIED, true));
        }
        if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isBlank()) {
            facts.add(new CandidateFact(profile, FactCategory.OTHER, profile.getPhoneNumber().trim(), "CANDIDATE_PROFILE", "phoneNumber", EvidenceLevel.VERIFIED, true));
        }
        if (profile.getCurrentLocation() != null && !profile.getCurrentLocation().isBlank()) {
            facts.add(new CandidateFact(profile, FactCategory.LOCATION, profile.getCurrentLocation().trim(), "CANDIDATE_PROFILE", "currentLocation", EvidenceLevel.VERIFIED, true));
        }
        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
            facts.add(new CandidateFact(profile, FactCategory.OTHER, profile.getLinkedinUrl().trim(), "CANDIDATE_PROFILE", "linkedinUrl", EvidenceLevel.VERIFIED, true));
        }
        if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
            facts.add(new CandidateFact(profile, FactCategory.OTHER, profile.getGithubUrl().trim(), "CANDIDATE_PROFILE", "githubUrl", EvidenceLevel.VERIFIED, true));
        }

        // 2. Candidate Skills
        List<CandidateSkill> skills = profile.getId() != null ? candidateSkillRepository.findByCandidateProfileId(profile.getId()) : Collections.emptyList();
        for (CandidateSkill cs : skills) {
            if (cs.getSkill() == null) continue;
            String skillName = cs.getSkill().getName().trim();
            EvidenceLevel level = "COMMERCIAL".equalsIgnoreCase(cs.getExperienceType()) ? EvidenceLevel.VERIFIED : EvidenceLevel.SUPPORTED;
            String refId = cs.getId() != null ? cs.getId().toString() : "skill-" + UUID.randomUUID();
            facts.add(new CandidateFact(profile, FactCategory.SKILL, skillName, "CANDIDATE_SKILL", refId, level, true));
            facts.add(new CandidateFact(profile, FactCategory.TECHNOLOGY, skillName, "CANDIDATE_SKILL", refId, level, true));
        }

        // 3. Candidate Experiences
        List<CandidateExperience> experiences = profile.getId() != null ? experienceRepository.findByCandidateProfileId(profile.getId()) : Collections.emptyList();
        for (CandidateExperience exp : experiences) {
            String company = exp.getCompany().trim();
            String expRef = exp.getId() != null ? exp.getId().toString() : "exp-" + UUID.randomUUID();
            facts.add(new CandidateFact(profile, FactCategory.EMPLOYMENT, company, "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
            if (exp.getRole() != null && !exp.getRole().isBlank()) {
                facts.add(new CandidateFact(profile, FactCategory.ROLE, exp.getRole().trim(), "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
            }
            if (exp.getDuration() != null && !exp.getDuration().isBlank()) {
                facts.add(new CandidateFact(profile, FactCategory.DATE, exp.getDuration().trim(), "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
            }

            // Extract technologies used in experience
            List<String> expTechs = parseJsonList(exp.getTechnologies());
            for (String t : expTechs) {
                if (!t.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.TECHNOLOGY, t.trim(), "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
                }
            }

            // Responsibilities & Achievements
            List<String> responsibilities = parseJsonList(exp.getResponsibilities());
            for (String r : responsibilities) {
                if (!r.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.RESPONSIBILITY, r.trim(), "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
                    extractAndAddMetrics(profile, r, expRef, facts);
                    extractAndAddTechFromText(profile, r, expRef, facts);
                }
            }

            List<String> achievements = parseJsonList(exp.getAchievements());
            for (String a : achievements) {
                if (!a.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.ACHIEVEMENT, a.trim(), "EXPERIENCE", expRef, EvidenceLevel.VERIFIED, true));
                    extractAndAddMetrics(profile, a, expRef, facts);
                    extractAndAddTechFromText(profile, a, expRef, facts);
                }
            }
        }

        // 4. Candidate Projects
        List<CandidateProject> projects = profile.getId() != null ? projectRepository.findByCandidateProfileId(profile.getId()) : Collections.emptyList();
        for (CandidateProject proj : projects) {
            String projName = proj.getName().trim();
            String projRef = proj.getId() != null ? proj.getId().toString() : "proj-" + UUID.randomUUID();
            facts.add(new CandidateFact(profile, FactCategory.PROJECT, projName, "PROJECT", projRef, EvidenceLevel.VERIFIED, true));

            List<String> projTechs = parseJsonList(proj.getTechnologies());
            for (String t : projTechs) {
                if (!t.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.TECHNOLOGY, t.trim(), "PROJECT", projRef, EvidenceLevel.SUPPORTED, true));
                }
            }

            if (proj.getArchitecture() != null && !proj.getArchitecture().isBlank()) {
                extractAndAddTechFromText(profile, proj.getArchitecture(), projRef, facts);
            }

            List<String> outcomes = parseJsonList(proj.getMeasurableOutcomes());
            for (String o : outcomes) {
                if (!o.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.ACHIEVEMENT, o.trim(), "PROJECT", projRef, EvidenceLevel.SUPPORTED, true));
                    extractAndAddMetrics(profile, o, projRef, facts);
                    extractAndAddTechFromText(profile, o, projRef, facts);
                }
            }

            List<String> pResp = parseJsonList(proj.getResponsibilities());
            for (String pr : pResp) {
                if (!pr.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.RESPONSIBILITY, pr.trim(), "PROJECT", projRef, EvidenceLevel.SUPPORTED, true));
                    extractAndAddMetrics(profile, pr, projRef, facts);
                    extractAndAddTechFromText(profile, pr, projRef, facts);
                }
            }
        }

        // 5. Candidate Education (Extracted strictly from verified Master Resume or Profile)
        extractCandidateEducation(profile, facts);

        // 6. Candidate Achievements (Extracted strictly from verified Master Resume or Profile)
        extractCandidateAchievements(profile, facts);

        // 7. Delete old facts and persist fresh canonical facts
        if (profile.getId() != null) {
            candidateFactRepository.deleteByCandidateProfileId(profile.getId());
        }
        List<CandidateFact> saved = candidateFactRepository.saveAll(facts);

        log.info("CANDIDATE_FACT_SYNC_COMPLETE - Synced [{}] canonical facts for Candidate [{}]",
                saved.size(), profile.getId());
        return saved;
    }

    /**
     * Retrieves all verified facts for a CandidateProfile.
     * If the database fact table is empty, runs a live sync.
     */
    @Transactional
    public List<CandidateFact> getCandidateFacts(CandidateProfile profile) {
        if (profile.getId() == null) {
            return syncCandidateFacts(profile);
        }
        List<CandidateFact> facts = candidateFactRepository.findByCandidateProfileId(profile.getId());
        if (facts.isEmpty()) {
            return syncCandidateFacts(profile);
        }
        return facts;
    }

    /**
     * Strict Organization Allowlist:
     * Extracts all allowed company, university, and institution names for this candidate.
     */
    public Set<String> getAllowedOrganizations(CandidateProfile profile) {
        List<CandidateFact> facts = getCandidateFacts(profile);
        Set<String> orgs = new HashSet<>();

        for (CandidateFact f : facts) {
            if (f.getCategory() == FactCategory.EMPLOYMENT || f.getCategory() == FactCategory.EDUCATION) {
                orgs.add(f.getValue().toLowerCase().trim());
            }
        }
        return orgs;
    }

    /**
     * Strict Degree Allowlist:
     * Extracts all verified educational degrees and credentials.
     * If candidate has no verified education, returns an EMPTY set!
     */
    public Set<String> getAllowedDegrees(CandidateProfile profile) {
        List<CandidateFact> facts = getCandidateFacts(profile);
        Set<String> degrees = new HashSet<>();

        for (CandidateFact f : facts) {
            if (f.getCategory() == FactCategory.EDUCATION) {
                String val = f.getValue().toLowerCase().trim();
                degrees.add(val);
                if (val.contains("bachelor") || val.contains("b.tech") || val.contains("technology")) {
                    degrees.add("bachelor");
                    degrees.add("b.tech");
                    degrees.add("bachelor of technology");
                }
                if (val.contains("class xii") || val.contains("xii") || val.contains("high school") || val.contains("12th")) {
                    degrees.add("class xii");
                    degrees.add("12th");
                }
            }
        }
        return degrees;
    }

    /**
     * Strict Technology / Skills Allowlist:
     * Returns all verified or project-supported technologies.
     */
    public Set<String> getAllowedTechnologies(CandidateProfile profile) {
        List<CandidateFact> facts = getCandidateFacts(profile);
        Set<String> techs = new HashSet<>();

        for (CandidateFact f : facts) {
            if (f.getCategory() == FactCategory.SKILL || f.getCategory() == FactCategory.TECHNOLOGY) {
                techs.add(f.getValue().toLowerCase().trim());
            }
        }
        return techs;
    }

    /**
     * Strict Metric Allowlist:
     * Returns all exact metric values (e.g., "30%", "1.8s", "sub-20ms", "5,000") verified in candidate history.
     */
    public Set<String> getAllowedMetrics(CandidateProfile profile) {
        List<CandidateFact> facts = getCandidateFacts(profile);
        Set<String> metrics = new HashSet<>();

        for (CandidateFact f : facts) {
            if (f.getCategory() == FactCategory.METRIC) {
                metrics.add(f.getValue().toLowerCase().trim());
            }
        }
        return metrics;
    }

    private void extractAndAddMetrics(CandidateProfile profile, String text, String sourceRef, List<CandidateFact> facts) {
        if (text == null || text.isBlank()) return;
        Matcher m = METRIC_PATTERN.matcher(text);
        while (m.find()) {
            String metric = m.group().trim();
            facts.add(new CandidateFact(profile, FactCategory.METRIC, metric, "METRIC_EXTRACTION", sourceRef, EvidenceLevel.VERIFIED, true));
        }
    }

    private static final List<String> COMMON_TECH_TOKENS = List.of(
            "Java", "Spring Boot", "Spring Security", "PostgreSQL", "SQL", "Docker", "Git", "REST APIs", "REST API",
            "REST", "JWT", "Microservices", "Microservice", "React", "TypeScript", "WebSocket", "Redis", "CI/CD",
            "Testcontainers", "OpenAPI", "Swagger", "Flyway", "Hibernate", "JPA"
    );

    private void extractAndAddTechFromText(CandidateProfile profile, String text, String sourceRef, List<CandidateFact> facts) {
        if (text == null || text.isBlank()) return;
        String lower = text.toLowerCase();
        for (String token : COMMON_TECH_TOKENS) {
            if (lower.contains(token.toLowerCase())) {
                facts.add(new CandidateFact(profile, FactCategory.TECHNOLOGY, token, "TEXT_EXTRACTION", sourceRef, EvidenceLevel.VERIFIED, true));
                facts.add(new CandidateFact(profile, FactCategory.SKILL, token, "TEXT_EXTRACTION", sourceRef, EvidenceLevel.VERIFIED, true));
            }
        }
    }

    private void extractCandidateEducation(CandidateProfile profile, List<CandidateFact> facts) {
        // 1. Check rawProfileData
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                JsonNode eduNode = root.path("education");
                if (eduNode.isArray() && !eduNode.isEmpty()) {
                    for (JsonNode item : eduNode) {
                        String degree = item.path("degree").asText("").trim();
                        String school = item.path("institution").asText("").trim();
                        if (!degree.isBlank()) {
                            facts.add(new CandidateFact(profile, FactCategory.EDUCATION, degree, "RAW_PROFILE", "education.degree", EvidenceLevel.VERIFIED, true));
                        }
                        if (!school.isBlank()) {
                            facts.add(new CandidateFact(profile, FactCategory.EDUCATION, school, "RAW_PROFILE", "education.institution", EvidenceLevel.VERIFIED, true));
                        }
                    }
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 2. Check Master Resume text for Education section
        Optional<Resume> masterResume = resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId());
        if (masterResume.isPresent() && masterResume.get().getRawExtractedText() != null) {
            String text = masterResume.get().getRawExtractedText();
            String[] lines = text.split("\r?\n");
            boolean inEduSection = false;

            for (String line : lines) {
                String trimmed = line.trim();
                String upper = trimmed.toUpperCase();
                if (upper.matches("^(EDUCATION|ACADEMIC\\s+BACKGROUND|DEGREES|QUALIFICATIONS).*")) {
                    inEduSection = true;
                    continue;
                } else if (inEduSection && upper.matches("^(EXPERIENCE|SKILLS|PROJECTS|CERTIFICATIONS|AWARDS|SUMMARY).*")) {
                    inEduSection = false;
                    break;
                }

                if (inEduSection && !trimmed.isBlank() && trimmed.length() < 120) {
                    Matcher dm = DEGREE_PATTERN.matcher(trimmed);
                    if (dm.find()) {
                        facts.add(new CandidateFact(profile, FactCategory.EDUCATION, trimmed, "MASTER_RESUME", "education", EvidenceLevel.VERIFIED, true));
                    } else if (trimmed.toLowerCase().contains("university") || trimmed.toLowerCase().contains("college") || trimmed.toLowerCase().contains("institute") || trimmed.toLowerCase().contains("technology")) {
                        facts.add(new CandidateFact(profile, FactCategory.EDUCATION, trimmed, "MASTER_RESUME", "institution", EvidenceLevel.VERIFIED, true));
                    }
                }
            }
        }
    }

    private void extractCandidateAchievements(CandidateProfile profile, List<CandidateFact> facts) {
        // 1. Check rawProfileData
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                JsonNode achNode = root.path("achievements");
                if (achNode.isArray() && !achNode.isEmpty()) {
                    for (JsonNode item : achNode) {
                        String title = item.path("title").asText("").trim();
                        String desc = item.path("description").asText("").trim();
                        String raw = item.path("rawText").asText("").trim();
                        String full = !title.isBlank() ? (title + (!desc.isBlank() ? ": " + desc : "")) : raw;
                        if (!full.isBlank()) {
                            facts.add(new CandidateFact(profile, FactCategory.ACHIEVEMENT, full, "RAW_PROFILE", "achievements", EvidenceLevel.VERIFIED, true));
                            if (!title.isBlank()) {
                                facts.add(new CandidateFact(profile, FactCategory.ACHIEVEMENT, title, "RAW_PROFILE", "achievements.title", EvidenceLevel.VERIFIED, true));
                            }
                            extractAndAddMetrics(profile, full, "achievements", facts);
                            extractAndAddTechFromText(profile, full, "achievements", facts);
                        }
                    }
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 2. Check Master Resume text fallback
        Optional<Resume> masterResume = resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId());
        if (masterResume.isPresent() && masterResume.get().getRawExtractedText() != null) {
            String text = masterResume.get().getRawExtractedText();
            String[] lines = text.split("\r?\n");
            boolean inAchSection = false;

            for (String line : lines) {
                String trimmed = line.trim();
                String upper = trimmed.toUpperCase();
                if (upper.matches("^(ACHIEVEMENTS|AWARDS|HONORS|HONOURS).*")) {
                    inAchSection = true;
                    continue;
                } else if (inAchSection && upper.matches("^(EXPERIENCE|SKILLS|PROJECTS|CERTIFICATIONS|EDUCATION|SUMMARY).*")) {
                    inAchSection = false;
                    break;
                }

                if (inAchSection && !trimmed.isBlank()) {
                    facts.add(new CandidateFact(profile, FactCategory.ACHIEVEMENT, trimmed, "MASTER_RESUME", "achievements", EvidenceLevel.VERIFIED, true));
                    extractAndAddMetrics(profile, trimmed, "achievements", facts);
                    extractAndAddTechFromText(profile, trimmed, "achievements", facts);
                }
            }
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
}
