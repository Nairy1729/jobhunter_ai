package com.jobhunter.controller;

import com.jobhunter.dto.matching.*;
import com.jobhunter.model.dto.*;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.JobService;
import com.jobhunter.service.discovery.DiscoveryService;
import com.jobhunter.service.matching.SemanticMatchingService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final DiscoveryService discoveryService;
    private final JobService jobService;
    private final SemanticMatchingService semanticMatchingService;
    private final com.jobhunter.service.tailoring.ResumeTailoringService resumeTailoringService;
    private final com.jobhunter.service.profile.ProfileReadinessService readinessService;

    public JobController(DiscoveryService discoveryService,
                         JobService jobService,
                         SemanticMatchingService semanticMatchingService,
                         com.jobhunter.service.tailoring.ResumeTailoringService resumeTailoringService,
                         com.jobhunter.service.profile.ProfileReadinessService readinessService) {
        this.discoveryService = discoveryService;
        this.jobService = jobService;
        this.semanticMatchingService = semanticMatchingService;
        this.resumeTailoringService = resumeTailoringService;
        this.readinessService = readinessService;
    }

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<DiscoverySummaryDto>> triggerDiscovery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) DiscoveryRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Authentication required"));
        }

        com.jobhunter.dto.profile.ProfileReadinessReport readiness = readinessService.evaluateReadiness(userDetails.getId());
        if (!readiness.isCanDiscover()) {
            return ResponseEntity.status(428).body(ApiResponse.error("PROFILE_REQUIRED: Candidate profile completeness check failed. Complete profile before discovering jobs."));
        }

        if (request == null) {
            request = new DiscoveryRequest(4, 10);
        }

        DiscoverySummaryDto summary = discoveryService.discoverJobs(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Job discovery execution completed", summary));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobDto>>> getJobs(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String freshness,
            @RequestParam(required = false, defaultValue = "RECOMMENDED") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Authentication required"));
        }

        com.jobhunter.dto.profile.ProfileReadinessReport readiness = readinessService.evaluateReadiness(userDetails.getId());
        if (!readiness.isCanDiscover()) {
            return ResponseEntity.status(428).body(ApiResponse.error("PROFILE_REQUIRED: Candidate profile must reach required completeness before viewing opportunities."));
        }

        Page<JobDto> jobs = jobService.findJobs(role, location, workMode, technology, source, minSalary, priority, freshness, sortBy, page, size, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getJobById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        JobDto job = jobService.getJobById(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Job retrieved successfully", job));
    }

    @GetMapping("/search-runs")
    public ResponseEntity<ApiResponse<List<SearchRunDto>>> getSearchRuns() {
        List<SearchRunDto> runs = jobService.getRecentSearchRuns();
        return ResponseEntity.ok(ApiResponse.ok("Recent search runs retrieved", runs));
    }

    // --- MILESTONE 3: SEMANTIC UNDERSTANDING & MATCHING ENDPOINTS ---

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<MatchAnalysisResponse>> analyzeJobMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchAnalysisResponse response = semanticMatchingService.analyzeAndMatch(id, userDetails.getId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Job match analysis evaluated successfully", response));
    }

    @GetMapping("/{id}/match")
    public ResponseEntity<ApiResponse<MatchAnalysisResponse>> getJobMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MatchAnalysisResponse response = semanticMatchingService.getMatchAnalysis(id, userDetails.getId())
                .orElseGet(() -> semanticMatchingService.analyzeAndMatch(id, userDetails.getId()));
        return ResponseEntity.ok(ApiResponse.ok("Job match analysis retrieved", response));
    }

    @GetMapping("/{id}/requirements")
    public ResponseEntity<ApiResponse<List<JobRequirementResponse>>> getJobRequirements(@PathVariable UUID id) {
        List<JobRequirementResponse> requirements = semanticMatchingService.getJobRequirements(id);
        return ResponseEntity.ok(ApiResponse.ok("Job requirements retrieved", requirements));
    }

    @GetMapping("/{id}/advantage")
    public ResponseEntity<ApiResponse<ApplicationAdvantageReportDto>> getAdvantageReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationAdvantageReportDto report = semanticMatchingService.getAdvantageReport(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Application Advantage Report retrieved", report));
    }

    @GetMapping("/{id}/tailoring")
    public ResponseEntity<ApiResponse<TailoringRecommendationDto>> getTailoringRecommendations(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TailoringRecommendationDto tailoring = semanticMatchingService.getTailoringRecommendations(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resume tailoring recommendations retrieved", tailoring));
    }

    // --- MILESTONE 4: INTELLIGENT RESUME TAILORING ENDPOINTS ---

    @PostMapping("/{id}/tailor")
    public ResponseEntity<ApiResponse<com.jobhunter.dto.tailoring.TailoredResumeResponse>> tailorResumeForJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        com.jobhunter.dto.tailoring.TailoredResumeResponse response = resumeTailoringService.tailorResume(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailored resume successfully generated", response));
    }

    @GetMapping("/{id}/tailor/plan")
    public ResponseEntity<ApiResponse<com.jobhunter.dto.tailoring.TailoringPlanDto>> getTailoringPlanForJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        com.jobhunter.dto.tailoring.TailoringPlanDto plan = resumeTailoringService.generateTailoringPlan(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Tailoring plan generated successfully", plan));
    }

    // --- APPLICATION STATE (ONLY: "Have I already applied to this job?") ---

    @PostMapping("/{id}/applied")
    public ResponseEntity<ApiResponse<JobAppliedStatusDto>> setAppliedStatus(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "true") Boolean applied,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isApplied = applied != null ? applied : true;
        JobAppliedStatusDto status = jobService.setJobAppliedStatus(id, userDetails.getId(), isApplied);
        return ResponseEntity.ok(ApiResponse.ok("Job application status updated", status));
    }
}
