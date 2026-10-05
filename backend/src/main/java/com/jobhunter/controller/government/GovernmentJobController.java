package com.jobhunter.controller.government;

import com.jobhunter.model.dto.ApiResponse;
import com.jobhunter.model.dto.government.*;
import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;
import com.jobhunter.security.CustomUserDetails;
import com.jobhunter.service.government.GovernmentDiscoveryService;
import com.jobhunter.service.government.GovernmentJobService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/government/jobs")
public class GovernmentJobController {

    private final GovernmentJobService jobService;
    private final GovernmentDiscoveryService discoveryService;

    public GovernmentJobController(GovernmentJobService jobService, GovernmentDiscoveryService discoveryService) {
        this.jobService = jobService;
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GovernmentJobDto>>> getGovernmentJobs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) GovernmentEmploymentType employmentType,
            @RequestParam(required = false, defaultValue = "OPEN") GovernmentJobStatus status,
            @RequestParam(required = false) GovernmentVerificationStatus verificationStatus,
            @RequestParam(required = false, defaultValue = "false") boolean includeUnverified,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "ALL") String eligibilityFilter,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        GovernmentJobFilterRequest request = new GovernmentJobFilterRequest();
        request.setState(state);
        request.setDistrict(district);
        request.setEmploymentType(employmentType);
        request.setStatus(status);
        request.setVerificationStatus(verificationStatus);
        request.setIncludeUnverified(includeUnverified);
        request.setEducation(education);
        request.setGender(gender);
        request.setQuery(query);
        request.setEligibilityFilter(eligibilityFilter);
        request.setPage(page);
        request.setSize(size);

        UUID userId = userDetails != null ? userDetails.getId() : null;
        Page<GovernmentJobDto> results = jobService.searchJobs(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Government recruitment opportunities retrieved", results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GovernmentJobDetailDto>> getJobDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        GovernmentJobDetailDto detail = jobService.getJobDetail(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Government job details retrieved", detail));
    }

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<GovernmentDiscoveryService.DiscoveryExecutionSummary>> triggerDiscovery(
            @RequestBody(required = false) GovernmentDiscoveryTriggerRequest request
    ) {
        String state = request != null ? request.getState() : null;
        String district = request != null ? request.getDistrict() : null;
        String keyword = request != null ? request.getSearchKeyword() : null;
        int maxQueries = request != null ? request.getMaxQueries() : 4;

        GovernmentDiscoveryService.DiscoveryExecutionSummary summary =
                discoveryService.executeDiscovery(state, district, keyword, maxQueries);

        return ResponseEntity.ok(ApiResponse.ok("Government discovery execution completed", summary));
    }
}
