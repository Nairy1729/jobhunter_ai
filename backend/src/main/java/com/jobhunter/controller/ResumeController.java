package com.jobhunter.controller;

import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.model.dto.ResumeUploadResponse;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Resume;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.ResumeRepository;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.ResumeParserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeParserService resumeParserService;
    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository profileRepository;

    public ResumeController(ResumeParserService resumeParserService,
                            ResumeRepository resumeRepository,
                            CandidateProfileRepository profileRepository) {
        this.resumeParserService = resumeParserService;
        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadMasterResume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws IOException {
        ResumeUploadResponse response = resumeParserService.uploadAndParseMasterResume(userDetails.getId(), file);
        return ResponseEntity.ok(ApiResponse.ok("Master resume uploaded and parsed successfully", response));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Resume>>> getResumes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CandidateProfile profile = profileRepository.findByUserId(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        List<Resume> resumes = resumeRepository.findByCandidateProfileId(profile.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resumes retrieved", resumes));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Resume>> getResume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with id: " + id));
        if (resume.getCandidateProfile() == null ||
            resume.getCandidateProfile().getUser() == null ||
            !resume.getCandidateProfile().getUser().getId().equals(userDetails.getId())) {
            throw new SecurityException("Resume not found or access denied: " + id);
        }
        return ResponseEntity.ok(ApiResponse.ok("Resume retrieved", resume));
    }
}
