package com.jobhunter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.CandidateProfileDto;
import com.jobhunter.model.dto.CandidateSkillDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final com.jobhunter.service.fact.CandidateFactStoreService factStoreService;
    private final ObjectMapper objectMapper;

    public CandidateProfileService(CandidateProfileRepository profileRepository,
                                   SkillRepository skillRepository,
                                   CandidateSkillRepository candidateSkillRepository,
                                   ResumeRepository resumeRepository,
                                   UserRepository userRepository,
                                   com.jobhunter.service.fact.CandidateFactStoreService factStoreService,
                                   ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.factStoreService = factStoreService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CandidateProfileDto getProfileByUserId(UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));

        return toDto(profile);
    }

    @Transactional
    public CandidateProfileDto updateProfile(UUID userId, CandidateProfileDto dto) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));

        // Update User name fields if provided
        User user = profile.getUser();
        boolean userChanged = false;
        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName().trim());
            userChanged = true;
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName().trim());
            userChanged = true;
        }
        if (userChanged) {
            userRepository.save(user);
        }

        if (dto.getHeadline() != null) profile.setHeadline(dto.getHeadline());
        if (dto.getSummary() != null) profile.setSummary(dto.getSummary());
        if (dto.getYearsOfExperience() != null) profile.setYearsOfExperience(dto.getYearsOfExperience());
        if (dto.getCurrentLocation() != null) profile.setCurrentLocation(dto.getCurrentLocation());
        if (dto.getMinSalaryInr() != null) profile.setMinSalaryInr(dto.getMinSalaryInr());
        if (dto.getCurrency() != null) profile.setCurrency(dto.getCurrency());
        if (dto.getGithubUrl() != null) profile.setGithubUrl(dto.getGithubUrl());
        if (dto.getLinkedinUrl() != null) profile.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getPortfolioUrl() != null) profile.setPortfolioUrl(dto.getPortfolioUrl());
        if (dto.getPhoneNumber() != null) profile.setPhoneNumber(dto.getPhoneNumber());

        try {
            if (dto.getPreferredLocations() != null) {
                profile.setPreferredLocations(objectMapper.writeValueAsString(dto.getPreferredLocations()));
            }
            if (dto.getWorkModes() != null) {
                profile.setWorkModes(objectMapper.writeValueAsString(dto.getWorkModes()));
            }
            if (dto.getTargetRoles() != null) {
                profile.setTargetRoles(objectMapper.writeValueAsString(dto.getTargetRoles()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize profile JSON fields", e);
        }

        CandidateProfile saved = profileRepository.save(profile);
        profileRepository.flush();

        // Refresh Canonical Fact Store with updated candidate facts
        try {
            factStoreService.syncCandidateFacts(saved);
        } catch (Exception ignored) {}

        return toDto(saved);
    }

    @Transactional
    public List<CandidateSkillDto> updateSkills(UUID userId, List<CandidateSkillDto> skillDtos) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));

        // 1. Delete old candidate skills and flush immediately to PostgreSQL
        candidateSkillRepository.deleteByCandidateProfileId(profile.getId());
        candidateSkillRepository.flush();
        profile.getSkills().clear();

        // 2. Deduplicate incoming skills by resolved skill ID and lowercase name
        java.util.Set<UUID> seenSkillIds = new java.util.HashSet<>();
        java.util.Set<String> seenSkillNames = new java.util.HashSet<>();
        List<CandidateSkill> newSkills = new ArrayList<>();

        if (skillDtos != null) {
            for (CandidateSkillDto dto : skillDtos) {
                if (dto.getSkillName() == null || dto.getSkillName().isBlank()) continue;
                String cleanName = dto.getSkillName().trim();
                if (!seenSkillNames.add(cleanName.toLowerCase())) {
                    continue;
                }

                Skill skill;
                if (dto.getSkillId() != null) {
                    skill = skillRepository.findById(dto.getSkillId())
                            .orElseGet(() -> findOrCreateSkill(cleanName, dto.getCategory()));
                } else {
                    skill = findOrCreateSkill(cleanName, dto.getCategory());
                }

                if (!seenSkillIds.add(skill.getId())) {
                    continue;
                }

                CandidateSkill cs = new CandidateSkill(
                        profile,
                        skill,
                        dto.getProficiencyLevel() != null ? dto.getProficiencyLevel() : "INTERMEDIATE",
                        dto.getYearsExperience() != null ? dto.getYearsExperience() : java.math.BigDecimal.valueOf(1.0),
                        dto.isPrimary(),
                        dto.getEvidenceText()
                );
                newSkills.add(cs);
            }
        }

        List<CandidateSkill> saved = candidateSkillRepository.saveAll(newSkills);
        candidateSkillRepository.flush();
        profile.getSkills().addAll(saved);

        // 3. Keep Canonical Fact Store in sync
        try {
            factStoreService.syncCandidateFacts(profile);
        } catch (Exception ignored) {}

        return saved.stream().map(this::toSkillDto).collect(Collectors.toList());
    }

    private Skill findOrCreateSkill(String name, String category) {
        String cleanName = name.trim();
        return skillRepository.findByNameIgnoreCase(cleanName)
                .orElseGet(() -> {
                    Skill s = new Skill(cleanName, category != null ? category : "CONCEPT", "[]");
                    return skillRepository.save(s);
                });
    }

    public CandidateProfileDto toDto(CandidateProfile profile) {
        CandidateProfileDto dto = new CandidateProfileDto();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setEmail(profile.getUser().getEmail());
        dto.setFirstName(profile.getUser().getFirstName());
        dto.setLastName(profile.getUser().getLastName());
        dto.setHeadline(profile.getHeadline());
        dto.setSummary(profile.getSummary());
        dto.setYearsOfExperience(profile.getYearsOfExperience());
        dto.setCurrentLocation(profile.getCurrentLocation());
        dto.setMinSalaryInr(profile.getMinSalaryInr());
        dto.setCurrency(profile.getCurrency());
        dto.setGithubUrl(profile.getGithubUrl());
        dto.setLinkedinUrl(profile.getLinkedinUrl());
        dto.setPortfolioUrl(profile.getPortfolioUrl());
        dto.setPhoneNumber(profile.getPhoneNumber());

        try {
            dto.setPreferredLocations(objectMapper.readValue(profile.getPreferredLocations(), new TypeReference<List<String>>() {}));
            dto.setWorkModes(objectMapper.readValue(profile.getWorkModes(), new TypeReference<List<String>>() {}));
            dto.setTargetRoles(objectMapper.readValue(profile.getTargetRoles(), new TypeReference<List<String>>() {}));
        } catch (Exception e) {
            dto.setPreferredLocations(new ArrayList<>());
            dto.setWorkModes(new ArrayList<>());
            dto.setTargetRoles(new ArrayList<>());
        }

        List<CandidateSkill> skills = candidateSkillRepository.findByCandidateProfileId(profile.getId());
        dto.setSkills(skills.stream().map(this::toSkillDto).collect(Collectors.toList()));

        resumeRepository.findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(profile.getId())
                .ifPresent(r -> {
                    dto.setMasterResumeId(r.getId());
                    dto.setMasterResumeTitle(r.getTitle());
                });

        return dto;
    }

    private CandidateSkillDto toSkillDto(CandidateSkill cs) {
        return new CandidateSkillDto(
                cs.getId(),
                cs.getSkill().getId(),
                cs.getSkill().getName(),
                cs.getSkill().getCategory(),
                cs.getProficiencyLevel(),
                cs.getYearsExperience(),
                cs.isPrimary(),
                cs.getEvidenceText()
        );
    }
}
