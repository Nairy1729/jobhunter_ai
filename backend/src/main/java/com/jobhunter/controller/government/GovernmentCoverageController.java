package com.jobhunter.controller.government;

import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.model.dto.government.GovernmentCoverageMetricsDto;
import com.jobhunter.service.government.GovernmentCoverageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/government/coverage")
public class GovernmentCoverageController {

    private final GovernmentCoverageService coverageService;

    public GovernmentCoverageController(GovernmentCoverageService coverageService) {
        this.coverageService = coverageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<GovernmentCoverageMetricsDto>> getCoverageMetrics() {
        GovernmentCoverageMetricsDto metrics = coverageService.getCoverageMetrics();
        return ResponseEntity.ok(ApiResponse.ok("Government coverage metrics retrieved", metrics));
    }
}
