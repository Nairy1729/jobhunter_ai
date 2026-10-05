package com.jobhunter.controller.government;

import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.model.dto.government.CandidateGovernmentProfileDto;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.government.CandidateGovernmentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/government/profile")
public class CandidateGovernmentProfileController {

    private final CandidateGovernmentProfileService profileService;

    public CandidateGovernmentProfileController(CandidateGovernmentProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CandidateGovernmentProfileDto>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required for candidate government profile"));
        }

        CandidateGovernmentProfileDto dto = profileService.getProfile(userDetails.getId())
                .orElseGet(CandidateGovernmentProfileDto::new);

        return ResponseEntity.ok(ApiResponse.ok("Candidate government profile retrieved", dto));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CandidateGovernmentProfileDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CandidateGovernmentProfileDto profileDto
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required for candidate government profile"));
        }

        CandidateGovernmentProfileDto saved = profileService.saveOrUpdateProfile(userDetails.getId(), profileDto);
        return ResponseEntity.ok(ApiResponse.ok("Candidate government profile updated successfully", saved));
    }
}
