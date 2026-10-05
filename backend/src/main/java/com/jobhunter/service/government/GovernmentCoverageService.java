package com.jobhunter.service.government;

import com.jobhunter.model.dto.government.GovernmentCoverageMetricsDto;
import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentSource;
import com.jobhunter.model.entity.government.GovernmentSourceType;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;
import com.jobhunter.repository.government.GovernmentJobRepository;
import com.jobhunter.repository.government.GovernmentSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GovernmentCoverageService {

    private final GovernmentSourceRepository sourceRepository;
    private final GovernmentJobRepository jobRepository;

    public GovernmentCoverageService(
            GovernmentSourceRepository sourceRepository,
            GovernmentJobRepository jobRepository) {
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public GovernmentCoverageMetricsDto getCoverageMetrics() {
        GovernmentCoverageMetricsDto metrics = new GovernmentCoverageMetricsDto();

        metrics.setTotalSources(sourceRepository.count());
        metrics.setActiveSources(sourceRepository.countByActiveTrue());
        metrics.setFailedSources(sourceRepository.countByStatus("FAILING"));

        // Find latest successful crawl across sources
        Instant latestCrawl = null;
        List<GovernmentSource> allSources = sourceRepository.findAll();
        for (GovernmentSource s : allSources) {
            if (s.getLastSuccessfulCrawl() != null) {
                if (latestCrawl == null || s.getLastSuccessfulCrawl().isAfter(latestCrawl)) {
                    latestCrawl = s.getLastSuccessfulCrawl();
                }
            }
        }
        metrics.setLastSuccessfulCrawl(latestCrawl != null ? latestCrawl : Instant.now());

        // Breakdown by source type
        metrics.setCentralGovernment(sourceRepository.countBySourceType(GovernmentSourceType.CENTRAL_GOVERNMENT));
        metrics.setStateGovernment(sourceRepository.countBySourceType(GovernmentSourceType.STATE_GOVERNMENT)
                + sourceRepository.countBySourceType(GovernmentSourceType.PSC));
        metrics.setDistrictAdministration(sourceRepository.countBySourceType(GovernmentSourceType.DISTRICT_ADMINISTRATION));
        metrics.setMunicipal(sourceRepository.countBySourceType(GovernmentSourceType.MUNICIPALITY));
        metrics.setPanchayat(sourceRepository.countBySourceType(GovernmentSourceType.PANCHAYAT));
        metrics.setUniversities(sourceRepository.countBySourceType(GovernmentSourceType.UNIVERSITY));
        metrics.setPsus(sourceRepository.countBySourceType(GovernmentSourceType.PSU));
        metrics.setDepartments(sourceRepository.countBySourceType(GovernmentSourceType.DEPARTMENT)
                + sourceRepository.countBySourceType(GovernmentSourceType.MINISTRY));
        metrics.setHealth(sourceRepository.countBySourceType(GovernmentSourceType.HEALTH));
        metrics.setEducation(sourceRepository.countBySourceType(GovernmentSourceType.EDUCATION));
        metrics.setWomenChildDevelopment(sourceRepository.countBySourceType(GovernmentSourceType.WOMEN_CHILD_DEVELOPMENT));
        metrics.setOther(sourceRepository.countBySourceType(GovernmentSourceType.OTHER)
                + sourceRepository.countBySourceType(GovernmentSourceType.MISSION)
                + sourceRepository.countBySourceType(GovernmentSourceType.SCHEME)
                + sourceRepository.countBySourceType(GovernmentSourceType.RECRUITMENT_BOARD)
                + sourceRepository.countBySourceType(GovernmentSourceType.RAILWAY)
                + sourceRepository.countBySourceType(GovernmentSourceType.BANKING));

        // Jobs metrics
        metrics.setTotalJobs(jobRepository.count());
        metrics.setVerifiedOfficialJobs(jobRepository.countByVerificationStatus(GovernmentVerificationStatus.VERIFIED_OFFICIAL));

        Map<String, Long> empBreakdown = new HashMap<>();
        List<Object[]> empCounts = jobRepository.countGroupByEmploymentType();
        long contractualCount = 0;
        long smallLocalCount = 0;
        for (Object[] row : empCounts) {
            if (row[0] != null && row[1] != null) {
                String typeStr = row[0].toString();
                long c = ((Number) row[1]).longValue();
                empBreakdown.put(typeStr, c);

                if (typeStr.equals("CONTRACTUAL") || typeStr.equals("SAMVIDA") || typeStr.equals("TEMPORARY") || typeStr.equals("HONORARIUM")) {
                    contractualCount += c;
                }
                if (typeStr.equals("PANCHAYAT_LEVEL") || typeStr.equals("BLOCK_LEVEL") || typeStr.equals("DISTRICT_LEVEL") || typeStr.equals("MUNICIPAL") || typeStr.equals("HONORARIUM") || typeStr.equals("SCHEME_BASED")) {
                    smallLocalCount += c;
                }
            }
        }
        metrics.setEmploymentTypeBreakdown(empBreakdown);
        metrics.setContractualSamvidaJobs(contractualCount);
        metrics.setSmallLocalJobs(smallLocalCount);

        Map<String, Long> verBreakdown = new HashMap<>();
        List<Object[]> verCounts = jobRepository.countGroupByVerificationStatus();
        for (Object[] row : verCounts) {
            if (row[0] != null && row[1] != null) {
                verBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        metrics.setVerificationStatusBreakdown(verBreakdown);

        return metrics;
    }
}
