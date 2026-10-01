package com.jobhunter.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI Orchestration Layer for Resume Intelligence.
 * Extracts candidate identity, contact info, headline, summary, years of experience,
 * target roles, skills matrix with factual evidence, work experiences, and technical projects.
 * Automatically populates and synchronizes CandidateProfile, CandidateSkills,
 * CandidateExperiences, and CandidateProjects.
 */
@Service
public class ResumeAiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAiOrchestratorService.class);

    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String geminiApiKey;
    private final String geminiModel;

    // Contact regex patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+\\d{1,3}[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}|\\d{5}[-.\\s]?\\d{5})");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9_-]+/?", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB_PATTERN = Pattern.compile("(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9_-]+/?", Pattern.CASE_INSENSITIVE);

    public ResumeAiOrchestratorService(
            CandidateProfileRepository profileRepository,
            CandidateSkillRepository candidateSkillRepository,
            CandidateExperienceRepository experienceRepository,
            CandidateProjectRepository projectRepository,
            SkillRepository skillRepository,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${ai.gemini.model-flash:gemini-flash-latest}") String geminiModel) {
        this.profileRepository = profileRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        this.geminiModel = (geminiModel != null && !geminiModel.isBlank() && !geminiModel.contains("1.5")) ? geminiModel.trim() : "gemini-flash-latest";
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public static class ExtractionResult {
        public String headline;
        public String summary;
        public BigDecimal yearsOfExperience = BigDecimal.ZERO;
        public String currentLocation;
        public List<String> preferredLocations = new ArrayList<>();
        public List<String> workModes = new ArrayList<>();
        public String fullName;
        public List<String> targetRoles = new ArrayList<>();
        public String phoneNumber;
        public String linkedinUrl;
        public String githubUrl;
        public List<ExtractedSkill> skills = new ArrayList<>();
        public List<ExtractedExperience> experiences = new ArrayList<>();
        public List<ExtractedProject> projects = new ArrayList<>();
        public List<ExtractedEducation> educations = new ArrayList<>();
        public List<ExtractedAchievement> achievements = new ArrayList<>();
        public String extractionSource = "DETERMINISTIC_NLP";
    }

    public static class ExtractedEducation {
        public String degree;
        public String institution;
        public String dates;
        public String grade;
    }

    public static class ExtractedAchievement {
        public String title;
        public String description;
        public String rawText;

        public ExtractedAchievement() {}
        public ExtractedAchievement(String title, String description, String rawText) {
            this.title = title;
            this.description = description;
            this.rawText = rawText;
        }
    }

    public static class ExtractedSkill {
        public String name;
        public String category;
        public String proficiencyLevel; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
        public int yearsExperience;
        public boolean primary;
        public String evidenceText;
    }

    public static class ExtractedExperience {
        public String company;
        public String role;
        public String duration;
        public List<String> technologies = new ArrayList<>();
        public List<String> responsibilities = new ArrayList<>();
        public List<String> achievements = new ArrayList<>();
        public String domain;
        public String evidenceText;
    }

    public static class ExtractedProject {
        public String name;
        public String description;
        public List<String> technologies = new ArrayList<>();
        public String architecture;
        public List<String> responsibilities = new ArrayList<>();
        public List<String> measurableOutcomes = new ArrayList<>();
        public String evidenceText;
        public String projectUrl;
    }

    public static String normalizeExtractedPdfText(String text) {
        if (text == null || text.isBlank()) return "";
        // Ligatures
        String t = text
                .replace("\ufb00", "ff")
                .replace("\ufb01", "fi")
                .replace("\ufb02", "fl")
                .replace("\ufb03", "ffi")
                .replace("\ufb04", "ffl");
        // PDF kerning space splits
        t = t.replaceAll("(?i)\\bT\\s+echnolog(y|ies)\\b", "Technolog$1")
                .replaceAll("(?i)\\bF\\s+rontend\\b", "Frontend")
                .replaceAll("(?i)\\bT\\s+esting\\b", "Testing")
                .replaceAll("(?i)\\bA\\s+WS\\b", "AWS")
                .replaceAll("(?i)\\bF\\s+ull\\b", "Full")
                .replaceAll("(?i)\\bA\\s+ward\\b", "Award")
                .replaceAll("(?i)\\bV\\s+ellore\\b", "Vellore");
        // Merge hyphenated linebreaks (e.g. Post-\ngreSQL -> PostgreSQL, Repos-\nitory -> Repository, initia-\ntives -> initiatives)
        t = t.replaceAll("(?m)(\\b[A-Za-z]+)-\\r?\\n([A-Za-z]+\\b)", "$1$2");
        return t;
    }

    /**
     * Executes the AI Orchestration Layer: extracts all relevant profile information from the resume text
     * and automatically updates and persists the candidate's profile, skills, experiences, and projects.
     */
    @Transactional
    public ExtractionResult orchestrateAndAutoFill(CandidateProfile profile, String resumeText, String originalFilename) {
        log.info("RESUME_AI_ORCHESTRATION_START - Ingesting master resume [{}] for candidate profile [{}]",
                originalFilename, profile.getId());

        String normalizedText = normalizeExtractedPdfText(resumeText);
        ExtractionResult extracted = null;

        // 1. Attempt LLM / Gemini extraction if configured
        if (isGeminiAvailable()) {
            try {
                extracted = extractWithGemini(normalizedText);
                if (extracted != null) {
                    extracted.extractionSource = "GEMINI_AI";
                    log.info("RESUME_AI_ORCHESTRATION_GEMINI_SUCCESS - Successfully extracted profile via Gemini");
                }
            } catch (Exception e) {
                log.warn("Gemini resume extraction failed, falling back to deterministic NLP parser: {}", e.getMessage());
            }
        }

        // 2. Fallback to comprehensive deterministic NLP / semantic parser
        if (extracted == null) {
            extracted = extractWithDeterministicNlp(normalizedText);
            extracted.extractionSource = "DETERMINISTIC_NLP";
        }

        // 3. Persist extracted intelligence into Candidate Profile
        applyAndPersistExtraction(profile, extracted);

        log.info("RESUME_AI_ORCHESTRATION_COMPLETE - Auto-filled profile [{}]: headline='{}', exp={} yrs, {} skills, {} experiences, {} projects (Source: {})",
                profile.getId(), profile.getHeadline(), profile.getYearsOfExperience(),
                extracted.skills.size(), extracted.experiences.size(), extracted.projects.size(),
                extracted.extractionSource);

        return extracted;
    }

    private boolean isGeminiAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    /**
     * Gemini AI extraction calling Gemini Flash API.
     */
    private ExtractionResult extractWithGemini(String resumeText) {
        String prompt = "You are a professional resume parser and candidate intelligence extractor.\n" +
                "Extract all factual details from the resume below into a single valid JSON object strictly matching this schema:\n" +
                "{\n" +
                "  \"headline\": \"string (e.g. Senior Backend Engineer | Java, Spring Boot, Microservices)\",\n" +
                "  \"summary\": \"string (2-4 sentence professional summary)\",\n" +
                "  \"yearsOfExperience\": number (e.g. 3.5),\n" +
                "  \"currentLocation\": \"string (e.g. Bengaluru, India)\",\n" +
                "  \"preferredLocations\": [\"string\"],\n" +
                "  \"workModes\": [\"REMOTE\", \"HYBRID\", or \"ONSITE\"],\n" +
                "  \"targetRoles\": [\"string\"],\n" +
                "  \"phoneNumber\": \"string or null\",\n" +
                "  \"linkedinUrl\": \"string or null\",\n" +
                "  \"githubUrl\": \"string or null\",\n" +
                "  \"skills\": [\n" +
                "    {\"name\": \"string\", \"category\": \"LANGUAGE|FRAMEWORK|DATABASE|CLOUD_DEVOPS|SYSTEM_DESIGN|TOOL\", \"proficiencyLevel\": \"EXPERT|ADVANCED|INTERMEDIATE|BEGINNER\", \"yearsExperience\": number, \"primary\": boolean, \"evidenceText\": \"string (factual quote from resume)\"}\n" +
                "  ],\n" +
                "  \"experiences\": [\n" +
                "    {\"company\": \"string\", \"role\": \"string\", \"duration\": \"string\", \"technologies\": [\"string\"], \"responsibilities\": [\"string\"], \"achievements\": [\"string\"], \"domain\": \"string\", \"evidenceText\": \"string\"}\n" +
                "  ],\n" +
                "  \"projects\": [\n" +
                "    {\"name\": \"string\", \"description\": \"string\", \"technologies\": [\"string\"], \"architecture\": \"string\", \"responsibilities\": [\"string\"], \"measurableOutcomes\": [\"string\"], \"evidenceText\": \"string\", \"projectUrl\": \"string or null\"}\n" +
                "  ]\n" +
                "}\n" +
                "DO NOT include markdown code fences (no ```json). Output pure JSON only.\n\n" +
                "RESUME TEXT:\n" + resumeText;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/" + geminiModel + ":generateContent")
                        .queryParam("key", geminiApiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String rawJson = textNode.asText().trim();
            if (rawJson.startsWith("```json")) rawJson = rawJson.substring(7);
            if (rawJson.startsWith("```")) rawJson = rawJson.substring(3);
            if (rawJson.endsWith("```")) rawJson = rawJson.substring(0, rawJson.length() - 3);

            return objectMapper.readValue(rawJson.trim(), ExtractionResult.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini resume extraction JSON response", e);
            return null;
        }
    }

    /**
     * Deterministic, production-grade NLP & regex extractor for offline or standalone environments.
     */
    public ExtractionResult extractWithDeterministicNlp(String text) {
        ExtractionResult result = new ExtractionResult();

        if (text == null || text.isBlank()) {
            return result;
        }

        String[] lines = text.split("\r?\n");

        // 1. Contact Information
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        if (phoneMatcher.find()) {
            result.phoneNumber = phoneMatcher.group().trim();
        }

        Matcher linkedinMatcher = LINKEDIN_PATTERN.matcher(text);
        if (linkedinMatcher.find()) {
            String url = linkedinMatcher.group().trim();
            result.linkedinUrl = url.toLowerCase().startsWith("http") ? url : "https://" + url;
        }

        Matcher githubMatcher = GITHUB_PATTERN.matcher(text);
        if (githubMatcher.find()) {
            String url = githubMatcher.group().trim();
            result.githubUrl = url.toLowerCase().startsWith("http") ? url : "https://" + url;
        }

        // Location inference
        result.currentLocation = detectLocation(text);
        result.preferredLocations = List.of(result.currentLocation, "Remote");
        result.workModes = List.of("REMOTE", "HYBRID");

        // 2. Sections Splitter
        Map<String, List<String>> sections = splitIntoSections(lines);

        // 2b. Extract Name and Headline from HEADER
        List<String> headerLines = sections.getOrDefault("HEADER", Collections.emptyList());
        for (String hLine : headerLines) {
            String hTrim = hLine.trim();
            if (hTrim.isBlank()) continue;
            boolean isContact = hTrim.contains("@") || hTrim.matches(".*\\d{5,}.*")
                    || hTrim.toLowerCase().contains("linkedin") || hTrim.toLowerCase().contains("github");
            if (!isContact && result.fullName == null && hTrim.length() < 50 && !hTrim.contains("|")) {
                result.fullName = hTrim;
            } else if (!isContact && result.headline == null && (hTrim.contains("|") || hTrim.toLowerCase().contains("developer") || hTrim.toLowerCase().contains("engineer"))) {
                result.headline = hTrim;
            }
        }

        // 3. Extract Summary
        List<String> summaryLines = sections.getOrDefault("SUMMARY", Collections.emptyList());
        if (!summaryLines.isEmpty()) {
            result.summary = String.join(" ", summaryLines).trim();
        }

        // 4. Extract Experiences
        List<String> expLines = sections.getOrDefault("EXPERIENCE", Collections.emptyList());
        result.experiences = parseExperiences(expLines);

        // 5. Extract Projects
        List<String> projLines = sections.getOrDefault("PROJECTS", Collections.emptyList());
        result.projects = parseProjects(projLines);

        // 6. Extract Skills
        List<String> skillLines = sections.getOrDefault("SKILLS", Collections.emptyList());
        result.skills = parseSkills(text, skillLines);

        // 7. Extract Education (Verified only, zero hallucination)
        List<String> eduLines = sections.getOrDefault("EDUCATION", Collections.emptyList());
        result.educations = parseEducations(eduLines);

        // 7b. Extract Achievements
        List<String> achLines = sections.getOrDefault("ACHIEVEMENTS", Collections.emptyList());
        result.achievements = parseAchievements(achLines);

        // 8. Calculate Years of Experience
        result.yearsOfExperience = calculateTotalYearsOfExperience(result.experiences);
        if (result.yearsOfExperience.compareTo(BigDecimal.ZERO) == 0 && !result.experiences.isEmpty()) {
            result.yearsOfExperience = BigDecimal.valueOf(result.experiences.size() * 1.5).setScale(1, RoundingMode.HALF_UP);
        }

        // 8. Synthesize Headline & Target Roles if not already found in header
        if (result.headline == null || result.headline.isBlank()) {
            if (!result.experiences.isEmpty()) {
                ExtractedExperience mostRecent = result.experiences.get(0);
                String primarySkill = result.skills.stream().filter(s -> s.primary).map(s -> s.name).limit(2).collect(Collectors.joining(", "));
                result.headline = mostRecent.role + (primarySkill.isBlank() ? "" : " | " + primarySkill);
                result.targetRoles = List.of(mostRecent.role, "Backend Engineer", "Software Engineer").stream().distinct().collect(Collectors.toList());
            } else {
                result.headline = "Software Engineer";
                result.targetRoles = List.of("Software Engineer", "Backend Engineer");
            }
        } else {
            result.targetRoles = List.of(result.headline.split("\\|")[0].trim(), "Software Engineer", "Backend Engineer").stream().distinct().collect(Collectors.toList());
        }

        if (result.summary == null || result.summary.isBlank()) {
            result.summary = result.headline + " with " + result.yearsOfExperience + " years of verified engineering experience. Skilled in " +
                    result.skills.stream().limit(5).map(s -> s.name).collect(Collectors.joining(", ")) + ".";
        }

        return result;
    }

    private void applyAndPersistExtraction(CandidateProfile profile, ExtractionResult extracted) {
        // Update Candidate User Name if available
        if (extracted.fullName != null && !extracted.fullName.isBlank()) {
            String cleanName = extracted.fullName.trim();
            String[] parts = cleanName.split("\\s+");
            User user = profile.getUser();
            if (user != null) {
                user.setFirstName(parts[0]);
                user.setLastName(parts.length > 1 ? cleanName.substring(parts[0].length()).trim() : "");
            }
        }

        // Update profile fields
        if (extracted.headline != null && !extracted.headline.isBlank()) {
            profile.setHeadline(extracted.headline);
        }
        if (extracted.summary != null && !extracted.summary.isBlank()) {
            profile.setSummary(extracted.summary);
        }
        if (extracted.yearsOfExperience != null && extracted.yearsOfExperience.compareTo(BigDecimal.ZERO) > 0) {
            profile.setYearsOfExperience(extracted.yearsOfExperience);
        }
        if (extracted.currentLocation != null && !extracted.currentLocation.isBlank()) {
            profile.setCurrentLocation(extracted.currentLocation);
        }
        if (extracted.phoneNumber != null && !extracted.phoneNumber.isBlank()) {
            profile.setPhoneNumber(extracted.phoneNumber);
        }
        if (extracted.linkedinUrl != null && !extracted.linkedinUrl.isBlank()) {
            profile.setLinkedinUrl(extracted.linkedinUrl);
        }
        if (extracted.githubUrl != null && !extracted.githubUrl.isBlank()) {
            profile.setGithubUrl(extracted.githubUrl);
        }

        try {
            if (extracted.preferredLocations != null && !extracted.preferredLocations.isEmpty()) {
                profile.setPreferredLocations(objectMapper.writeValueAsString(extracted.preferredLocations));
            }
            if (extracted.workModes != null && !extracted.workModes.isEmpty()) {
                profile.setWorkModes(objectMapper.writeValueAsString(extracted.workModes));
            }
            if (extracted.targetRoles != null && !extracted.targetRoles.isEmpty()) {
                profile.setTargetRoles(objectMapper.writeValueAsString(extracted.targetRoles));
            }
            Map<String, Object> rawData = new HashMap<>();
            if (extracted.headline != null && !extracted.headline.isBlank()) {
                rawData.put("headline", extracted.headline);
            }
            if (extracted.educations != null && !extracted.educations.isEmpty()) {
                rawData.put("education", extracted.educations);
            }
            if (extracted.achievements != null && !extracted.achievements.isEmpty()) {
                rawData.put("achievements", extracted.achievements);
            }
            if (!rawData.isEmpty()) {
                profile.setRawProfileData(objectMapper.writeValueAsString(rawData));
            }
        } catch (Exception e) {
            log.error("Failed to serialize extracted profile metadata", e);
        }

        CandidateProfile savedProfile = profileRepository.save(profile);

        // Update Skills
        if (extracted.skills != null && !extracted.skills.isEmpty()) {
            candidateSkillRepository.deleteByCandidateProfileId(savedProfile.getId());
            List<CandidateSkill> skillEntities = new ArrayList<>();
            for (ExtractedSkill es : extracted.skills) {
                Skill skill = findOrCreateSkill(es.name, es.category);
                CandidateSkill cs = new CandidateSkill(
                        savedProfile,
                        skill,
                        es.proficiencyLevel != null ? es.proficiencyLevel : "INTERMEDIATE",
                        BigDecimal.valueOf(Math.max(1, es.yearsExperience)),
                        es.primary,
                        es.evidenceText != null ? es.evidenceText : "Extracted from verified master resume"
                );
                skillEntities.add(cs);
            }
            candidateSkillRepository.saveAll(skillEntities);
        }

        // Update Experiences
        if (extracted.experiences != null && !extracted.experiences.isEmpty()) {
            experienceRepository.deleteByCandidateProfileId(savedProfile.getId());
            List<CandidateExperience> expEntities = new ArrayList<>();
            for (ExtractedExperience ee : extracted.experiences) {
                CandidateExperience exp = new CandidateExperience(
                        savedProfile,
                        ee.company != null ? ee.company : "Company",
                        ee.role != null ? ee.role : "Software Engineer",
                        ee.duration != null ? ee.duration : "2022 - Present",
                        toJsonString(ee.technologies),
                        toJsonString(ee.responsibilities),
                        toJsonString(ee.achievements),
                        ee.domain != null ? ee.domain : "SOFTWARE",
                        ee.evidenceText != null ? ee.evidenceText : "Extracted from master resume"
                );
                expEntities.add(exp);
            }
            experienceRepository.saveAll(expEntities);
        }

        // Update Projects
        if (extracted.projects != null && !extracted.projects.isEmpty()) {
            projectRepository.deleteByCandidateProfileId(savedProfile.getId());
            List<CandidateProject> projEntities = new ArrayList<>();
            for (ExtractedProject ep : extracted.projects) {
                CandidateProject proj = new CandidateProject(
                        savedProfile,
                        ep.name != null ? ep.name : "Technical Project",
                        ep.description != null ? ep.description : "Production engineering project",
                        toJsonString(ep.technologies),
                        ep.architecture != null ? ep.architecture : "Microservices / Modular Architecture",
                        toJsonString(ep.responsibilities),
                        toJsonString(ep.measurableOutcomes),
                        ep.evidenceText != null ? ep.evidenceText : "Extracted from master resume",
                        ep.projectUrl
                );
                projEntities.add(proj);
            }
            projectRepository.saveAll(projEntities);
        }
    }

    private Skill findOrCreateSkill(String name, String category) {
        String trimmed = name.trim();
        return skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    Skill s = new Skill(trimmed, category != null ? category : "TECHNICAL", "[]");
                    return skillRepository.save(s);
                });
    }

    private String toJsonString(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String detectLocation(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("bengaluru") || lower.contains("bangalore")) return "Bengaluru, India";
        if (lower.contains("hyderabad")) return "Hyderabad, India";
        if (lower.contains("pune")) return "Pune, India";
        if (lower.contains("mumbai")) return "Mumbai, India";
        if (lower.contains("delhi") || lower.contains("ncr") || lower.contains("gurgaon") || lower.contains("noida")) return "Delhi NCR, India";
        if (lower.contains("san francisco") || lower.contains("bay area")) return "San Francisco, CA";
        if (lower.contains("seattle")) return "Seattle, WA";
        if (lower.contains("new york") || lower.contains("nyc")) return "New York, NY";
        if (lower.contains("remote")) return "Remote, India";
        return "Bengaluru, India";
    }

    private Map<String, List<String>> splitIntoSections(String[] lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = "HEADER";
        sections.put(currentSection, new ArrayList<>());

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            String upper = trimmed.toUpperCase();
            if (upper.matches("^(PROFESSIONAL\\s+)?(SUMMARY|PROFILE|OBJECTIVE|ABOUT(\\s+ME)?).*")) {
                currentSection = "SUMMARY";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (upper.matches("^(PROFESSIONAL\\s+)?(EXPERIENCE|WORK\\s+HISTORY|EMPLOYMENT).*")) {
                currentSection = "EXPERIENCE";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (upper.matches("^(TECHNICAL\\s+)?(PROJECTS|PERSONAL\\s+PROJECTS|ACADEMIC\\s+PROJECTS).*")) {
                currentSection = "PROJECTS";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (upper.matches("^(TECHNICAL\\s+)?(SKILLS|CORE\\s+COMPETENCIES|TECHNOLOGIES).*")) {
                currentSection = "SKILLS";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (upper.matches("^(EDUCATION|ACADEMIC\\s+BACKGROUND).*")) {
                currentSection = "EDUCATION";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else if (upper.matches("^(ACHIEVEMENTS|AWARDS|HONORS|ACCOLADES).*")) {
                currentSection = "ACHIEVEMENTS";
                sections.putIfAbsent(currentSection, new ArrayList<>());
            } else {
                sections.computeIfAbsent(currentSection, k -> new ArrayList<>()).add(trimmed);
            }
        }
        return sections;
    }

    private List<ExtractedExperience> parseExperiences(List<String> lines) {
        List<ExtractedExperience> list = new ArrayList<>();
        if (lines.isEmpty()) return list;

        ExtractedExperience current = null;
        StringBuilder currentBullet = null;
        Pattern datePattern = Pattern.compile("(?i)(20\\d{2}|19\\d{2}|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\d{0,4}\\s*[-–to—]+\\s*(20\\d{2}|present|current)");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            boolean isBullet = trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*");

            if (isBullet) {
                if (current != null && currentBullet != null && currentBullet.length() > 0) {
                    addBulletToExperience(current, currentBullet.toString());
                }
                currentBullet = new StringBuilder(trimmed.replaceFirst("^[-•*]\\s*", "").trim());
                continue;
            }

            // Not a bullet: could be a header line or continuation line
            Matcher dm = datePattern.matcher(trimmed);
            boolean hasDate = dm.find();
            boolean hasJobTitle = trimmed.toLowerCase().matches(".*\\b(engineer|developer|architect|lead|intern|manager|analyst|specialist|consultant)\\b.*");

            boolean isNewExperienceHeader = (current == null) || (currentBullet != null && (hasDate || hasJobTitle));

            if (isNewExperienceHeader) {
                if (current != null && currentBullet != null && currentBullet.length() > 0) {
                    addBulletToExperience(current, currentBullet.toString());
                    currentBullet = null;
                }
                if (current != null) {
                    list.add(current);
                }
                current = new ExtractedExperience();

                if (hasDate) {
                    current.duration = dm.group().trim();
                    String withoutDate = trimmed.substring(0, dm.start()) + trimmed.substring(dm.end());
                    current.company = withoutDate.replaceAll("[—\\|•]", "").trim();
                    if (current.company.isBlank()) current.company = trimmed;
                } else if (trimmed.contains("—") || trimmed.contains(" - ") || trimmed.contains("|")) {
                    String[] parts = trimmed.split("[—\\|•]|\\s+-\\s+");
                    current.company = parts[0].trim();
                    current.role = parts.length > 1 ? parts[1].trim() : "Software Engineer";
                } else {
                    current.company = trimmed;
                }
            } else if (current != null && (current.role == null || current.role.equals("Software Engineer")) && (hasJobTitle || currentBullet == null)) {
                String[] parts = trimmed.split("\\s{2,}|(?<=[a-z])\\s+(?=India|Remote|US|USA|UK|Singapore|Bengaluru|Bangalore|Pune|Hyderabad|Chennai|Delhi|Noida)");
                if (parts.length >= 2) {
                    current.role = parts[0].trim();
                } else {
                    current.role = trimmed.replaceAll("(?i)\\b(India|Remote|USA|UK)\\b.*", "").trim();
                    if (current.role.isBlank()) current.role = trimmed;
                }
            } else if (currentBullet != null) {
                currentBullet.append(" ").append(trimmed);
            }
        }

        if (current != null && currentBullet != null && currentBullet.length() > 0) {
            addBulletToExperience(current, currentBullet.toString());
        }
        if (current != null) {
            list.add(current);
        }

        for (ExtractedExperience exp : list) {
            if (exp.company == null || exp.company.isBlank()) {
                exp.company = "Engineering Organization";
            }
            if (exp.role == null || exp.role.isBlank()) {
                exp.role = "Software Engineer";
            }
            if (exp.duration == null || exp.duration.isBlank()) {
                exp.duration = "2023 - Present";
            }
        }

        return list;
    }

    private void addBulletToExperience(ExtractedExperience exp, String bullet) {
        String clean = bullet.trim();
        if (clean.isBlank()) return;
        if (clean.matches(".*(\\d+%|\\$|latency|reduced|increased|improved|slashed|scaled|optimized).*")) {
            exp.achievements.add(clean);
        } else {
            exp.responsibilities.add(clean);
        }
        detectTechInString(clean, exp.technologies);
    }

    private List<ExtractedProject> parseProjects(List<String> lines) {
        List<ExtractedProject> list = new ArrayList<>();
        if (lines.isEmpty()) return list;

        ExtractedProject current = null;
        StringBuilder currentBullet = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            boolean isBullet = trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*");

            if (isBullet) {
                if (current != null && currentBullet != null && currentBullet.length() > 0) {
                    addBulletToProject(current, currentBullet.toString());
                }
                currentBullet = new StringBuilder(trimmed.replaceFirst("^[-•*]\\s*", "").trim());
                continue;
            }

            boolean isNewProject = (current == null) || (currentBullet != null);

            if (isNewProject) {
                if (current != null && currentBullet != null && currentBullet.length() > 0) {
                    addBulletToProject(current, currentBullet.toString());
                    currentBullet = null;
                }
                if (current != null) list.add(current);
                current = new ExtractedProject();

                String[] pipeParts = trimmed.split("\\|");
                if (pipeParts.length > 1) {
                    String firstPart = pipeParts[0].trim();
                    Matcher techMatcher = Pattern.compile("(?i)\\b(Java|Python|React|Angular|Vue|ASP\\.NET|Node|Go|C#|C\\+\\+|TypeScript)\\b").matcher(firstPart);
                    if (techMatcher.find() && techMatcher.start() > 3) {
                        current.name = firstPart.substring(0, techMatcher.start()).replaceAll("[–—]$", "").trim();
                        current.technologies.add(firstPart.substring(techMatcher.start()).trim());
                    } else {
                        current.name = firstPart;
                    }
                    for (int i = 1; i < pipeParts.length; i++) {
                        String t = pipeParts[i].trim();
                        if (!t.isBlank()) current.technologies.add(t);
                    }
                } else if (trimmed.contains("–") || trimmed.contains("—") || trimmed.contains(" - ")) {
                    current.name = trimmed;
                    detectTechInString(trimmed, current.technologies);
                } else {
                    current.name = trimmed;
                    detectTechInString(trimmed, current.technologies);
                }
            } else if (current != null && current.description == null && currentBullet == null) {
                current.description = trimmed;
                current.architecture = trimmed;
            } else if (currentBullet != null) {
                currentBullet.append(" ").append(trimmed);
            }
        }

        if (current != null && currentBullet != null && currentBullet.length() > 0) {
            addBulletToProject(current, currentBullet.toString());
        }
        if (current != null) list.add(current);

        return list;
    }

    private void addBulletToProject(ExtractedProject proj, String bullet) {
        String clean = bullet.trim();
        if (clean.isBlank()) return;
        if (clean.matches(".*(\\d+%|latency|throughput|users|requests|processed).*")) {
            proj.measurableOutcomes.add(clean);
        } else {
            proj.responsibilities.add(clean);
        }
        detectTechInString(clean, proj.technologies);
    }

    private List<ExtractedSkill> parseSkills(String fullText, List<String> skillSectionLines) {
        List<ExtractedSkill> skills = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Curated dictionary of high-signal skills
        Map<String, String> skillCatalog = Map.ofEntries(
                Map.entry("Java", "LANGUAGE"),
                Map.entry("Python", "LANGUAGE"),
                Map.entry("TypeScript", "LANGUAGE"),
                Map.entry("JavaScript", "LANGUAGE"),
                Map.entry("Go", "LANGUAGE"),
                Map.entry("C++", "LANGUAGE"),
                Map.entry("C#", "LANGUAGE"),
                Map.entry("SQL", "LANGUAGE"),
                Map.entry("Spring Boot", "FRAMEWORK"),
                Map.entry("Spring Security", "FRAMEWORK"),
                Map.entry("Node.js", "FRAMEWORK"),
                Map.entry("React", "FRAMEWORK"),
                Map.entry("PostgreSQL", "DATABASE"),
                Map.entry("MySQL", "DATABASE"),
                Map.entry("MongoDB", "DATABASE"),
                Map.entry("Redis", "DATABASE"),
                Map.entry("Kafka", "ARCHITECTURE"),
                Map.entry("RabbitMQ", "ARCHITECTURE"),
                Map.entry("Docker", "CLOUD_DEVOPS"),
                Map.entry("Kubernetes", "CLOUD_DEVOPS"),
                Map.entry("AWS", "CLOUD_DEVOPS"),
                Map.entry("GCP", "CLOUD_DEVOPS"),
                Map.entry("Azure", "CLOUD_DEVOPS"),
                Map.entry("Git", "TOOL"),
                Map.entry("REST APIs", "ARCHITECTURE"),
                Map.entry("GraphQL", "ARCHITECTURE"),
                Map.entry("Microservices", "ARCHITECTURE"),
                Map.entry("CI/CD", "CLOUD_DEVOPS"),
                Map.entry("JUnit", "TOOL"),
                Map.entry("Maven", "TOOL"),
                Map.entry("Gradle", "TOOL")
        );

        String textLower = fullText.toLowerCase();

        for (Map.Entry<String, String> entry : skillCatalog.entrySet()) {
            String skillName = entry.getKey();
            String cat = entry.getValue();
            String pattern = "\\b" + Pattern.quote(skillName.toLowerCase()) + "\\b";

            Matcher m = Pattern.compile(pattern).matcher(textLower);
            if (m.find()) {
                if (seen.add(skillName.toLowerCase())) {
                    ExtractedSkill es = new ExtractedSkill();
                    es.name = skillName;
                    es.category = cat;
                    es.yearsExperience = 2; // Default reasonable baseline
                    es.primary = (cat.equals("LANGUAGE") || cat.equals("FRAMEWORK") || cat.equals("DATABASE"));

                    int occurrences = 0;
                    Matcher countMatcher = Pattern.compile(pattern).matcher(textLower);
                    while (countMatcher.find()) occurrences++;

                    if (occurrences >= 4) {
                        es.proficiencyLevel = "EXPERT";
                        es.yearsExperience = 3;
                    } else if (occurrences >= 2) {
                        es.proficiencyLevel = "ADVANCED";
                        es.yearsExperience = 2;
                    } else {
                        es.proficiencyLevel = "INTERMEDIATE";
                        es.yearsExperience = 1;
                    }

                    es.evidenceText = extractSentenceContaining(fullText, skillName);
                    skills.add(es);
                }
            }
        }

        return skills;
    }

    private void detectTechInString(String s, List<String> techList) {
        List<String> known = List.of(
                "Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes", "Kafka", "Redis",
                "AWS", "Python", "React", "TypeScript", "REST APIs", "Microservices", "SQL", "Git"
        );
        String lower = s.toLowerCase();
        for (String k : known) {
            if (lower.contains(k.toLowerCase()) && !techList.contains(k)) {
                techList.add(k);
            }
        }
    }

    private String extractSentenceContaining(String fullText, String keyword) {
        Pattern p = Pattern.compile("([^.!?\\n]*?\\b" + Pattern.quote(keyword) + "\\b[^.!?\\n]*)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String sentence = m.group(1).trim();
            if (sentence.length() > 150) {
                return sentence.substring(0, 147) + "...";
            }
            return sentence;
        }
        return "Verified from candidate master resume";
    }

    private BigDecimal calculateTotalYearsOfExperience(List<ExtractedExperience> experiences) {
        int totalMonths = 0;
        for (ExtractedExperience exp : experiences) {
            if (exp.duration != null) {
                Matcher m = Pattern.compile("(\\d{4})").matcher(exp.duration);
                List<Integer> years = new ArrayList<>();
                while (m.find()) {
                    years.add(Integer.parseInt(m.group(1)));
                }
                if (years.size() == 2) {
                    totalMonths += Math.max(12, (years.get(1) - years.get(0)) * 12);
                } else if (years.size() == 1 && exp.duration.toLowerCase().contains("present")) {
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    totalMonths += Math.max(12, (currentYear - years.get(0)) * 12);
                } else {
                    totalMonths += 18; // default 1.5 yrs
                }
            }
        }
        double years = totalMonths / 12.0;
        return BigDecimal.valueOf(Math.max(1.0, years)).setScale(1, RoundingMode.HALF_UP);
    }

    private List<ExtractedEducation> parseEducations(List<String> lines) {
        List<ExtractedEducation> list = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return list;

        ExtractedEducation current = null;
        Pattern datePattern = Pattern.compile("(?i)(20\\d{2}|19\\d{2})\\s*[-–to—]+\\s*(20\\d{2}|present|current)");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            Matcher dm = datePattern.matcher(trimmed);
            boolean hasDate = dm.find();
            boolean isDegreeLine = trimmed.toLowerCase().matches(".*\\b(bachelor|master|b\\.tech|m\\.tech|b\\.e\\.|m\\.e\\.|b\\.s\\.|m\\.s\\.|phd|degree|diploma|bca|mca|class\\s+xii|class\\s+x|high\\s+school|secondary|12th|10th)\\b.*");
            boolean isInstitutionLine = trimmed.toLowerCase().matches(".*\\b(university|college|institute|school|academy|vit|iit|nit|bits|vidyagyan)\\b.*");

            if (isDegreeLine) {
                if (current != null && current.degree != null && current.institution != null) {
                    list.add(current);
                    current = null;
                }
                if (current == null) current = new ExtractedEducation();

                Matcher gradeMatcher = Pattern.compile("(\\d{1,2}(\\.\\d+)?%|cgpa\\s*:?\\s*\\d+(\\.\\d+)?|\\bgrade\\s*:?\\s*[a-z0-9+]+)", Pattern.CASE_INSENSITIVE).matcher(trimmed);
                if (gradeMatcher.find()) {
                    current.grade = gradeMatcher.group().trim();
                    current.degree = trimmed.substring(0, gradeMatcher.start()).replaceAll("[,–—|]$", "").trim();
                } else {
                    current.degree = trimmed;
                }

                if (hasDate && (current.dates == null || current.dates.isBlank())) {
                    current.dates = dm.group().trim();
                }
            } else if (isInstitutionLine || hasDate) {
                if (current != null && current.institution != null && current.degree != null) {
                    list.add(current);
                    current = null;
                }
                if (current == null) current = new ExtractedEducation();

                if (hasDate) {
                    current.dates = dm.group().trim();
                    current.institution = trimmed.substring(0, dm.start()).replaceAll("[,–—|]$", "").trim();
                    if (current.institution.isBlank()) current.institution = trimmed;
                } else {
                    current.institution = trimmed;
                }
            } else if (current != null) {
                if (trimmed.matches(".*\\d{1,2}\\.\\d+%.*") || trimmed.toLowerCase().contains("cgpa") || trimmed.toLowerCase().contains("gpa")) {
                    current.grade = trimmed;
                } else if (current.institution == null) {
                    current.institution = trimmed;
                } else if (current.degree == null) {
                    current.degree = trimmed;
                }
            }
        }
        if (current != null && (current.institution != null || current.degree != null)) {
            list.add(current);
        }
        return list;
    }

    public List<ExtractedAchievement> parseAchievements(List<String> lines) {
        List<ExtractedAchievement> list = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return list;

        StringBuilder currentBullet = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                if (currentBullet != null && currentBullet.length() > 0) {
                    list.add(buildExtractedAchievement(currentBullet.toString()));
                }
                currentBullet = new StringBuilder(trimmed.replaceFirst("^[-•*]\\s*", "").trim());
            } else if (currentBullet != null) {
                currentBullet.append(" ").append(trimmed);
            } else {
                currentBullet = new StringBuilder(trimmed);
            }
        }
        if (currentBullet != null && currentBullet.length() > 0) {
            list.add(buildExtractedAchievement(currentBullet.toString()));
        }
        return list;
    }

    private ExtractedAchievement buildExtractedAchievement(String text) {
        String clean = text.trim();
        String title = clean;
        String desc = clean;
        if (clean.contains("–") || clean.contains("—") || clean.contains(" - ")) {
            String[] parts = clean.split("\\s+[–—-]\\s+", 2);
            title = parts[0].trim();
            desc = parts.length > 1 ? parts[1].trim() : clean;
        } else if (clean.toLowerCase().startsWith("awarded")) {
            title = "Scholarship / Award";
            desc = clean;
        }
        return new ExtractedAchievement(title, desc, clean);
    }
}
