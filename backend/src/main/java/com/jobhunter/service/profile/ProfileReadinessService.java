package com.jobhunter.service.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.profile.ProfileReadinessReport;
import com.jobhunter.dto.profile.ProfileReadinessState;
import com.jobhunter.dto.profile.ReadinessItem;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ProfileReadinessService {

    private static final Logger log = LoggerFactory.getLogger(ProfileReadinessService.class);

    private final CandidateProfileRepository profileRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "(?i)\\b(bachelor(?:'s)?|master(?:'s)?|b\\.tech|m\\.tech|b\\.e\\.|m\\.e\\.|b\\.s\\.|m\\.s\\.|ph\\.?d\\.?|diploma|associate|bca|mca|degree)\\b"
    );

    public ProfileReadinessService(
            CandidateProfileRepository profileRepository,
            CandidateExperienceRepository experienceRepository,
            CandidateSkillRepository candidateSkillRepository,
            ResumeRepository resumeRepository,
            ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.experienceRepository = experienceRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProfileReadinessReport evaluateReadiness(UUID userId) {
        Optional<CandidateProfile> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            ProfileReadinessReport report = new ProfileReadinessReport(
                    ProfileReadinessState.PROFILE_NOT_READY,
                    0,
                    false,
                    "YOUR PROFILE ISN'T READY",
                    "JobHunter needs to understand you before it can find jobs worth your time."
            );
            report.getMissingItems().add("Candidate profile record does not exist.");
            return report;
        }

        return evaluateProfile(profileOpt.get());
    }

    @Transactional(readOnly = true)
    public ProfileReadinessReport evaluateProfile(CandidateProfile profile) {
        List<ReadinessItem> items = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int score = 0;

        // 1. Master Resume (15%)
        Optional<Resume> masterResume = profile.getId() != null
                ? resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId())
                : Optional.empty();
        boolean hasResume = masterResume.isPresent() && masterResume.get().getRawExtractedText() != null
                && masterResume.get().getRawExtractedText().trim().length() >= 50;

        if (hasResume) {
            score += 15;
            items.add(new ReadinessItem("resume", "Resume", true, true, "Master resume parsed and verified."));
        } else {
            items.add(new ReadinessItem("resume", "Resume", false, true, "Master resume not uploaded or unparsed."));
            missing.add("Resume");
        }

        // 2. Commercial / Professional Experience (15%)
        List<CandidateExperience> experiences = profile.getId() != null
                ? experienceRepository.findByCandidateProfileId(profile.getId())
                : Collections.emptyList();
        boolean hasExperience = !experiences.isEmpty();

        if (hasExperience) {
            score += 15;
            items.add(new ReadinessItem("experience", "Experience", true, true, experiences.size() + " position(s) recorded."));
        } else {
            items.add(new ReadinessItem("experience", "Experience", false, true, "No commercial or professional experience added."));
            missing.add("Experience");
        }

        // 3. Verified Skills & Technologies (15%)
        List<CandidateSkill> skills = profile.getId() != null
                ? candidateSkillRepository.findByCandidateProfileId(profile.getId())
                : Collections.emptyList();
        boolean hasSkills = skills.size() >= 3;

        if (hasSkills) {
            score += 15;
            items.add(new ReadinessItem("skills", "Skills", true, true, skills.size() + " verified skill(s) recorded."));
        } else {
            items.add(new ReadinessItem("skills", "Skills", false, true, "At least 3 verified technical skills required (current: " + skills.size() + ")."));
            missing.add("Skills");
        }

        // 4. Education (15%)
        boolean hasEducation = checkEducation(profile, masterResume);
        if (hasEducation) {
            score += 15;
            items.add(new ReadinessItem("education", "Education", true, true, "Educational background verified."));
        } else {
            items.add(new ReadinessItem("education", "Education", false, true, "Educational qualification or degree missing."));
            missing.add("Education");
        }

        // 5. Target Roles (15%)
        List<String> targetRoles = parseJsonList(profile.getTargetRoles());
        boolean hasTargetRoles = !targetRoles.isEmpty() || (profile.getHeadline() != null && !profile.getHeadline().isBlank());
        if (hasTargetRoles) {
            score += 15;
            items.add(new ReadinessItem("targetRoles", "Target roles", true, true, targetRoles.isEmpty() ? profile.getHeadline() : String.join(", ", targetRoles)));
        } else {
            items.add(new ReadinessItem("targetRoles", "Target roles", false, true, "Target job roles or title not specified."));
            missing.add("Target roles");
        }

        // 6. Location Preference (10%)
        List<String> preferredLocs = parseJsonList(profile.getPreferredLocations());
        boolean hasLocation = !preferredLocs.isEmpty() || (profile.getCurrentLocation() != null && !profile.getCurrentLocation().isBlank());
        if (hasLocation) {
            score += 10;
            items.add(new ReadinessItem("location", "Location preference", true, true, preferredLocs.isEmpty() ? profile.getCurrentLocation() : String.join(", ", preferredLocs)));
        } else {
            items.add(new ReadinessItem("location", "Location preference", false, true, "Preferred job locations or base location missing."));
            missing.add("Location preference");
        }

        // 7. Work Mode Preference (10%)
        List<String> workModes = parseJsonList(profile.getWorkModes());
        boolean hasWorkMode = !workModes.isEmpty();
        if (hasWorkMode) {
            score += 10;
            items.add(new ReadinessItem("workMode", "Work preference", true, true, String.join(", ", workModes)));
        } else {
            items.add(new ReadinessItem("workMode", "Work preference", false, true, "Work preference (Remote, Hybrid, Onsite) not chosen."));
            missing.add("Work preference");
        }

        // 8. Experience Level (5%)
        boolean hasYoE = profile.getYearsOfExperience() != null && profile.getYearsOfExperience().compareTo(BigDecimal.ZERO) > 0;
        if (hasYoE) {
            score += 5;
            items.add(new ReadinessItem("experienceLevel", "Experience level", true, false, profile.getYearsOfExperience() + " years of experience recorded."));
        } else {
            items.add(new ReadinessItem("experienceLevel", "Experience level", false, false, "Years of experience not specified."));
        }

        // Optional bonus: Minimum Salary (+5%)
        if (profile.getMinSalaryInr() != null && profile.getMinSalaryInr().compareTo(BigDecimal.ZERO) > 0) {
            items.add(new ReadinessItem("salaryExpectations", "Salary expectations", true, false, "₹" + profile.getMinSalaryInr() + " INR"));
        }

        score = Math.min(100, score);
        boolean isReady = missing.isEmpty() && score >= 80;

        ProfileReadinessState state = isReady ? ProfileReadinessState.PROFILE_READY : ProfileReadinessState.PROFILE_NOT_READY;
        String headline = isReady ? "PROFILE READY" : "YOUR PROFILE ISN'T READY";
        String message = isReady
                ? "JobHunter understands enough about you to start finding relevant opportunities."
                : "JobHunter needs to understand you before it can find jobs worth your time.";

        ProfileReadinessReport report = new ProfileReadinessReport(state, score, isReady, headline, message);
        report.setItems(items);
        report.setMissingItems(missing);

        return report;
    }

    private boolean checkEducation(CandidateProfile profile, Optional<Resume> masterResume) {
        // 1. Check rawProfileData
        if (profile.getRawProfileData() != null && !profile.getRawProfileData().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(profile.getRawProfileData());
                JsonNode edu = root.path("education");
                if (edu.isArray() && !edu.isEmpty()) {
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // 2. Check Master Resume text
        if (masterResume.isPresent() && masterResume.get().getRawExtractedText() != null) {
            String text = masterResume.get().getRawExtractedText();
            if (DEGREE_PATTERN.matcher(text).find() || text.toLowerCase().contains("university") || text.toLowerCase().contains("college") || text.toLowerCase().contains("b.tech") || text.toLowerCase().contains("b.e.")) {
                return true;
            }
        }

        // 3. Check summary or headline
        String combined = ((profile.getSummary() != null ? profile.getSummary() : "") + " " + (profile.getHeadline() != null ? profile.getHeadline() : "")).toLowerCase();
        return DEGREE_PATTERN.matcher(combined).find();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String clean = json.replaceAll("[\\[\\]\"']", "").trim();
            if (clean.isBlank()) return Collections.emptyList();
            return Arrays.stream(clean.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
    }
}
