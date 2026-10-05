package com.jobhunter.controller;

import com.jobhunter.dto.tailoring.offerpilot.CreateTailoredResumeRequest;
import com.jobhunter.dto.tailoring.offerpilot.TailoredResumeDetailResponse;
import com.jobhunter.dto.tailoring.offerpilot.TailoredResumeSummaryResponse;
import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.tailoring.offerpilot.OfferPilotTailoringEngineService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * OfferPilot AI Resume Tailoring REST API Controller.
 * Implements Section 9 of the OfferPilot Architecture Blueprint.
 */
@RestController
@RequestMapping("/api/v1/tailored-resumes")
public class OfferPilotTailoredResumeController {

    private final OfferPilotTailoringEngineService tailoringEngineService;

    public OfferPilotTailoredResumeController(OfferPilotTailoringEngineService tailoringEngineService) {
        this.tailoringEngineService = tailoringEngineService;
    }

    /**
     * 1. POST /api/v1/tailored-resumes
     * Tailors candidate master resume against target job description.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TailoredResumeDetailResponse>> createTailoredResume(
            @RequestBody CreateTailoredResumeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoredResumeDetailResponse response = tailoringEngineService.createTailoredResume(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume generated successfully", response));
    }

    /**
     * 2. GET /api/v1/tailored-resumes
     * Returns summary list of all tailored resumes for authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TailoredResumeSummaryResponse>>> listTailoredResumes(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TailoredResumeSummaryResponse> list = tailoringEngineService.listTailoredResumes(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resumes retrieved successfully", list));
    }

    /**
     * 3. GET /api/v1/tailored-resumes/{id}
     * Returns full detail of tailored resume.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TailoredResumeDetailResponse>> getTailoredResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoredResumeDetailResponse response = tailoringEngineService.getTailoredResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume details retrieved successfully", response));
    }

    /**
     * 4. POST /api/v1/tailored-resumes/{id}/render-latex
     * Re-renders and writes resume.tex to file storage.
     */
    @PostMapping("/{id}/render-latex")
    public ResponseEntity<ApiResponse<TailoredResumeDetailResponse>> renderLatex(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoredResumeDetailResponse response = tailoringEngineService.renderLatexForResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("LaTeX rendered and saved successfully", response));
    }

    /**
     * 5. GET /api/v1/tailored-resumes/{id}/download-latex
     * Serves the generated .tex file.
     */
    @GetMapping("/{id}/download-latex")
    public ResponseEntity<Resource> downloadLatex(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        File texFile = tailoringEngineService.getLatexFile(id, userDetails.getId());
        Resource resource = new FileSystemResource(texFile);

        String filename = (texFile.getName() != null && !texFile.getName().contains("tailored-resume"))
                ? texFile.getName()
                : "resume.tex";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/x-tex"))
                .contentLength(texFile.length())
                .body(resource);
    }

    /**
     * 6. DELETE /api/v1/tailored-resumes/{id}
     * Deletes the record and associated files.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTailoredResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        tailoringEngineService.deleteTailoredResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume deleted successfully", null));
    }
}
