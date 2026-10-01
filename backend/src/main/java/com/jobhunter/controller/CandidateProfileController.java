package com.jobhunter.controller;

import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.model.dto.CandidateProfileDto;
import com.jobhunter.model.dto.CandidateSkillDto;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.CandidateProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class CandidateProfileController {

    private final CandidateProfileService profileService;
    private final com.jobhunter.service.profile.ProfileReadinessService readinessService;

    public CandidateProfileController(CandidateProfileService profileService,
                                      com.jobhunter.service.profile.ProfileReadinessService readinessService) {
        this.profileService = profileService;
        this.readinessService = readinessService;
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<com.jobhunter.dto.profile.ProfileReadinessReport>> getProfileReadiness(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        com.jobhunter.dto.profile.ProfileReadinessReport report = readinessService.evaluateReadiness(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile readiness evaluated", report));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CandidateProfileDto>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfileDto profile = profileService.getProfileByUserId(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CandidateProfileDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CandidateProfileDto dto) {
        CandidateProfileDto updated = profileService.updateProfile(userDetails.getId(), dto);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @PutMapping("/skills")
    public ResponseEntity<ApiResponse<List<CandidateSkillDto>>> updateSkills(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<CandidateSkillDto> skillDtos) {
        List<CandidateSkillDto> updatedSkills = profileService.updateSkills(userDetails.getId(), skillDtos);
        return ResponseEntity.ok(ApiResponse.ok("Skills updated successfully", updatedSkills));
    }
}
