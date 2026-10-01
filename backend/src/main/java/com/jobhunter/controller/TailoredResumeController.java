package com.jobhunter.controller;

import com.jobhunter.dto.tailoring.ResumeValidationReport;
import com.jobhunter.dto.tailoring.TailoredResumeResponse;
import com.jobhunter.dto.tailoring.TailoringDiffDto;
import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.tailoring.ResumeTailoringService;
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

@RestController
@RequestMapping("/api/tailored-resumes")
public class TailoredResumeController {

    private final ResumeTailoringService resumeTailoringService;
    private final com.jobhunter.service.tailoring.offerpilot.OfferPilotTailoringEngineService tailoringEngineService;

    public TailoredResumeController(
            ResumeTailoringService resumeTailoringService,
            com.jobhunter.service.tailoring.offerpilot.OfferPilotTailoringEngineService tailoringEngineService) {
        this.resumeTailoringService = resumeTailoringService;
        this.tailoringEngineService = tailoringEngineService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TailoredResumeResponse>> getTailoredResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoredResumeResponse response = resumeTailoringService.getTailoredResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume retrieved successfully", response));
    }

    @GetMapping("/{id}/changes")
    public ResponseEntity<ApiResponse<TailoringDiffDto>> getTailoringChanges(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoringDiffDto diff = resumeTailoringService.getTailoringDiff(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resume tailoring diff retrieved successfully", diff));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ApiResponse<ResumeValidationReport>> validateTailoredResume(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ResumeValidationReport report = resumeTailoringService.validateTailoredResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume validation completed", report));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadTailoredPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            File pdfFile = resumeTailoringService.getPdfFile(id, userDetails.getId());
            Resource resource = new FileSystemResource(pdfFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfFile.getName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.length())
                    .body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422)
                    .body(ApiResponse.error("Download blocked: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/download-latex")
    public ResponseEntity<Resource> downloadTailoredLatex(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        File texFile = tailoringEngineService.getLatexFile(id, userDetails.getId());
        Resource resource = new FileSystemResource(texFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tailored-resume.tex\"")
                .contentType(MediaType.parseMediaType("application/x-tex"))
                .contentLength(texFile.length())
                .body(resource);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<TailoredResumeResponse>>> getTailoredResumesForJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TailoredResumeResponse> list = resumeTailoringService.getTailoredResumesForJob(jobId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resumes for job retrieved", list));
    }
}
