package com.jobhunter.service.government;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.government.CandidateGovernmentProfileDto;
import com.jobhunter.model.entity.User;
import com.jobhunter.model.entity.government.CandidateGovernmentProfile;
import com.jobhunter.repository.UserRepository;
import com.jobhunter.repository.government.CandidateGovernmentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CandidateGovernmentProfileService {

    private final CandidateGovernmentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CandidateGovernmentProfileService(
            CandidateGovernmentProfileRepository profileRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<CandidateGovernmentProfileDto> getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<CandidateGovernmentProfile> getProfileEntity(UUID userId) {
        return profileRepository.findByUserId(userId);
    }

    @Transactional
    public CandidateGovernmentProfileDto saveOrUpdateProfile(UUID userId, CandidateGovernmentProfileDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        CandidateGovernmentProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new CandidateGovernmentProfile(user));

        profile.setAge(dto.getAge());
        profile.setDob(dto.getDob());
        profile.setGender(dto.getGender());
        profile.setState(dto.getState());
        profile.setDistrict(dto.getDistrict());
        profile.setDomicileState(dto.getDomicileState());
        profile.setDomicileDistrict(dto.getDomicileDistrict());
        profile.setHighestEducation(dto.getHighestEducation());
        profile.setPassingYear(dto.getPassingYear());
        profile.setYearsOfExperience(dto.getYearsOfExperience() != null ? dto.getYearsOfExperience() : BigDecimal.ZERO);
        profile.setCategory(dto.getCategory() != null ? dto.getCategory() : "UR/GEN");
        profile.setPwd(dto.isPwd());
        profile.setExServiceman(dto.isExServiceman());

        try {
            profile.setDegreesJson(objectMapper.writeValueAsString(dto.getDegrees() != null ? dto.getDegrees() : Collections.emptyList()));
            profile.setPreferredStatesJson(objectMapper.writeValueAsString(dto.getPreferredStates() != null ? dto.getPreferredStates() : Collections.emptyList()));
            profile.setPreferredDistrictsJson(objectMapper.writeValueAsString(dto.getPreferredDistricts() != null ? dto.getPreferredDistricts() : Collections.emptyList()));
            profile.setPreferredEmploymentTypesJson(objectMapper.writeValueAsString(dto.getPreferredEmploymentTypes() != null ? dto.getPreferredEmploymentTypes() : Collections.emptyList()));
            profile.setSkillsJson(objectMapper.writeValueAsString(dto.getSkills() != null ? dto.getSkills() : Collections.emptyList()));
        } catch (Exception e) {
            // Keep default empty JSON arrays on serialization failure
        }

        CandidateGovernmentProfile saved = profileRepository.save(profile);
        return toDto(saved);
    }

    public CandidateGovernmentProfileDto toDto(CandidateGovernmentProfile entity) {
        CandidateGovernmentProfileDto dto = new CandidateGovernmentProfileDto();
        dto.setAge(entity.getAge());
        dto.setDob(entity.getDob());
        dto.setGender(entity.getGender());
        dto.setState(entity.getState());
        dto.setDistrict(entity.getDistrict());
        dto.setDomicileState(entity.getDomicileState());
        dto.setDomicileDistrict(entity.getDomicileDistrict());
        dto.setHighestEducation(entity.getHighestEducation());
        dto.setPassingYear(entity.getPassingYear());
        dto.setYearsOfExperience(entity.getYearsOfExperience());
        dto.setCategory(entity.getCategory());
        dto.setPwd(entity.isPwd());
        dto.setExServiceman(entity.isExServiceman());

        dto.setDegrees(parseJsonList(entity.getDegreesJson()));
        dto.setPreferredStates(parseJsonList(entity.getPreferredStatesJson()));
        dto.setPreferredDistricts(parseJsonList(entity.getPreferredDistrictsJson()));
        dto.setPreferredEmploymentTypes(parseJsonList(entity.getPreferredEmploymentTypesJson()));
        dto.setSkills(parseJsonList(entity.getSkillsJson()));

        return dto;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
