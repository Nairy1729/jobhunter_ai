package com.jobhunter.service.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.client.firecrawl.FirecrawlClient;
import com.jobhunter.client.firecrawl.dto.*;
import com.jobhunter.client.firecrawl.exception.FirecrawlApiKeyMissingException;
import com.jobhunter.model.dto.DiscoveryRequest;
import com.jobhunter.model.dto.DiscoverySummaryDto;
import com.jobhunter.model.dto.JobDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.discovery.adapter.JobSourceAdapter;
import com.jobhunter.service.discovery.adapter.JobSourceAdapterRegistry;
import com.jobhunter.service.discovery.dto.ExtractedJobDetails;
import com.jobhunter.service.discovery.dto.GeneratedSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final CandidateProfileRepository candidateProfileRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobSourceRepository jobSourceRepository;
    private final SearchRunRepository searchRunRepository;
    private final AgentRunRepository agentRunRepository;
    private final FirecrawlClient firecrawlClient;
    private final JobSearchQueryGenerator queryGenerator;
    private final UrlNormalizer urlNormalizer;
    private final JobSourceAdapterRegistry adapterRegistry;
    private final JobQualityFilter jobQualityFilter;
    private final DeduplicationService deduplicationService;
    private final JobExtractorService jobExtractorService;
    private final com.jobhunter.service.profile.ProfileReadinessService profileReadinessService;
    private final ObjectMapper objectMapper;

    public DiscoveryService(
            CandidateProfileRepository candidateProfileRepository,
            JobRepository jobRepository,
            CompanyRepository companyRepository,
            JobSourceRepository jobSourceRepository,
            SearchRunRepository searchRunRepository,
            AgentRunRepository agentRunRepository,
            FirecrawlClient firecrawlClient,
            JobSearchQueryGenerator queryGenerator,
            UrlNormalizer urlNormalizer,
            JobSourceAdapterRegistry adapterRegistry,
            JobQualityFilter jobQualityFilter,
            DeduplicationService deduplicationService,
            JobExtractorService jobExtractorService,
            com.jobhunter.service.profile.ProfileReadinessService profileReadinessService,
            ObjectMapper objectMapper) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.searchRunRepository = searchRunRepository;
        this.agentRunRepository = agentRunRepository;
        this.firecrawlClient = firecrawlClient;
        this.queryGenerator = queryGenerator;
        this.urlNormalizer = urlNormalizer;
        this.adapterRegistry = adapterRegistry;
        this.jobQualityFilter = jobQualityFilter;
        this.deduplicationService = deduplicationService;
        this.jobExtractorService = jobExtractorService;
        this.profileReadinessService = profileReadinessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DiscoverySummaryDto discoverJobs(UUID userId, DiscoveryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("DISCOVERY_STARTED - Candidate User ID: [{}]", userId);

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        com.jobhunter.dto.profile.ProfileReadinessReport readiness = profileReadinessService.evaluateProfile(profile);
        if (!readiness.isCanDiscover()) {
            throw new IllegalStateException("PROFILE_REQUIRED: Candidate profile completeness check failed (" + readiness.getScore() + "%). Complete profile before discovering jobs.");
        }

        DiscoverySummaryDto summary = new DiscoverySummaryDto();
        List<Job> newlyCreatedJobs = new ArrayList<>();

        int maxQueries = (request != null && request.getMaxQueries() != null) ? request.getMaxQueries() : 4;
        int limitPerQuery = (request != null && request.getSearchLimitPerQuery() != null) ? request.getSearchLimitPerQuery() : 10;

        List<GeneratedSearchQuery> queries = queryGenerator.generateQueries(profile, maxQueries);

        if (request != null && request.getSources() != null && !request.getSources().isEmpty()) {
            Set<String> requestedSources = new HashSet<>(request.getSources());
            queries = queries.stream()
                    .filter(q -> requestedSources.contains(q.getSourceIdentifier()))
                    .toList();
        }

        // Check Firecrawl API key before launching queries
        if (!firecrawlClient.isAvailable()) {
            String msg = "FIRECRAWL_API_KEY environment variable is not configured. Please set FIRECRAWL_API_KEY to execute web discovery.";
            log.warn("DISCOVERY_ABORTED - {}", msg);
            summary.getErrors().add(msg);
            summary.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            AgentRun failedRun = new AgentRun("DiscoveryAgent", "ABORTED", System.currentTimeMillis() - startTime);
            failedRun.setErrorMessage(msg);
            agentRunRepository.save(failedRun);

            return summary;
        }

        for (GeneratedSearchQuery query : queries) {
            JobSource jobSource = resolveJobSource(query.getSourceIdentifier());

            SearchRun searchRun = new SearchRun();
            searchRun.setJobSource(jobSource);
            searchRun.setQueryString(query.getQueryString());
            searchRun.setStatus("RUNNING");
            searchRun.setStartedAt(Instant.now());
            try {
                searchRun.setParameters(objectMapper.writeValueAsString(Map.of(
                        "userId", userId.toString(),
                        "roles", query.getRoles(),
                        "skills", query.getSkills(),
                        "locations", query.getLocations()
                )));
            } catch (Exception ignored) {}
            searchRun = searchRunRepository.save(searchRun);

            int discoveredInRun = 0;
            int ingestedInRun = 0;

            try {
                FirecrawlSearchRequest searchRequest = new FirecrawlSearchRequest(query.getQueryString(), limitPerQuery);
                FirecrawlSearchResponse searchResponse = firecrawlClient.search(searchRequest);
                summary.setQueriesExecuted(summary.getQueriesExecuted() + 1);

                List<FirecrawlDocument> documents = searchResponse.getData() != null ? searchResponse.getData() : List.of();
                discoveredInRun = documents.size();
                summary.setSearchResultsFound(summary.getSearchResultsFound() + discoveredInRun);

                for (FirecrawlDocument doc : documents) {
                    String rawUrl = doc.getUrl();
                    if (rawUrl == null || rawUrl.isBlank()) {
                        continue;
                    }

                    // 1. SSRF and preliminary quality filter
                    if (!jobQualityFilter.isPromisingUrl(rawUrl, doc.getTitle())) {
                        summary.setInvalidPagesSkipped(summary.getInvalidPagesSkipped() + 1);
                        continue;
                    }

                    JobSourceAdapter adapter = adapterRegistry.findAdapter(rawUrl);
                    String canonicalUrl = adapter.canonicalizeUrl(rawUrl);

                    // 2. Tier 1 Deduplication Check: canonical URL hash
                    String canonicalHash = deduplicationService.generateCanonicalUrlHash(canonicalUrl);
                    if (deduplicationService.isDuplicateCanonicalUrl(canonicalHash)) {
                        summary.setDuplicatesSkipped(summary.getDuplicatesSkipped() + 1);
                        continue;
                    }

                    // 3. Scrape the page
                    FirecrawlDocument scrapeDoc = null;
                    try {
                        FirecrawlScrapeRequest scrapeRequest = new FirecrawlScrapeRequest(canonicalUrl);
                        FirecrawlScrapeResponse scrapeResponse = firecrawlClient.scrape(scrapeRequest);
                        summary.setPagesScraped(summary.getPagesScraped() + 1);

                        if (scrapeResponse != null && scrapeResponse.getData() != null) {
                            scrapeDoc = scrapeResponse.getData();
                            if (scrapeDoc.getUrl() == null) {
                                scrapeDoc.setUrl(canonicalUrl);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Scrape failed for [{}]: {}", canonicalUrl, e.getMessage());
                    }

                    // Fallback to search markdown if scrape failed or was empty
                    if (scrapeDoc == null || scrapeDoc.getMarkdown() == null || scrapeDoc.getMarkdown().isBlank()) {
                        if (doc.getMarkdown() != null && !doc.getMarkdown().isBlank()) {
                            scrapeDoc = doc;
                            if (scrapeDoc.getUrl() == null) {
                                scrapeDoc.setUrl(canonicalUrl);
                            }
                        } else {
                            summary.setInvalidPagesSkipped(summary.getInvalidPagesSkipped() + 1);
                            continue;
                        }
                    }

                    // 4. Quality filter on scraped content
                    Integer statusCode = scrapeDoc.getMetadata() != null ? scrapeDoc.getMetadata().getStatusCode() : 200;
                    if (!jobQualityFilter.isQualityJobPage(canonicalUrl, scrapeDoc.getTitle(), scrapeDoc.getMarkdown(), statusCode)) {
                        summary.setInvalidPagesSkipped(summary.getInvalidPagesSkipped() + 1);
                        continue;
                    }

                    // 5. Job extraction
                    ExtractedJobDetails details = jobExtractorService.extractJobDetails(scrapeDoc, adapter);

                    // 6. Tier 2 Deduplication Check: content hash
                    if (deduplicationService.isDuplicateContent(details.getContentHash())) {
                        summary.setDuplicatesSkipped(summary.getDuplicatesSkipped() + 1);
                        continue;
                    }

                    // 6b. Tier 3 Deduplication Check: Cross-source opportunity
                    if (deduplicationService.isDuplicateOpportunity(
                            details.getCompanyName(),
                            details.getTitle(),
                            details.getLocation(),
                            details.getRawDescriptionMarkdown())) {
                        summary.setDuplicatesSkipped(summary.getDuplicatesSkipped() + 1);
                        continue;
                    }

                    // 7. Persist fresh job
                    Company company = companyRepository.findByNameIgnoreCase(details.getCompanyName())
                            .orElseGet(() -> {
                                Company newComp = new Company(details.getCompanyName());
                                newComp.setAtsProvider(adapter.getSourceIdentifier());
                                return companyRepository.save(newComp);
                            });

                    Job job = new Job();
                    job.setCompany(company);
                    job.setJobSource(jobSource);
                    job.setSearchRun(searchRun);
                    job.setTitle(details.getTitle());
                    job.setNormalizedTitle(details.getNormalizedTitle());
                    job.setLocation(details.getLocation());
                    job.setWorkMode(details.getWorkMode());
                    job.setEmploymentType(details.getEmploymentType());
                    job.setMinExperienceYears(details.getMinExperienceYears());
                    job.setMaxExperienceYears(details.getMaxExperienceYears());
                    job.setMinSalary(details.getMinSalary());
                    job.setMaxSalary(details.getMaxSalary());
                    job.setSalaryCurrency(details.getSalaryCurrency());
                    job.setJobUrl(details.getJobUrl());
                    job.setCanonicalUrl(details.getCanonicalUrl());
                    job.setCanonicalUrlHash(details.getCanonicalUrlHash());
                    job.setContentHash(details.getContentHash());
                    job.setRawDescriptionMarkdown(details.getRawDescriptionMarkdown());

                    try {
                        job.setStructuredJobSpec(objectMapper.writeValueAsString(Map.of(
                                "responsibilities", details.getResponsibilities(),
                                "requiredQualifications", details.getRequiredQualifications(),
                                "preferredQualifications", details.getPreferredQualifications(),
                                "detectedTechnologies", details.getDetectedTechnologies()
                        )));
                    } catch (Exception ignored) {}

                    job.setPostingDate(details.getPostingDate());
                    job.setActive(true);
                    job.setPipelineStatus("DISCOVERED");

                    Job savedJob = jobRepository.save(job);
                    newlyCreatedJobs.add(savedJob);
                    ingestedInRun++;
                    summary.setJobsCreated(summary.getJobsCreated() + 1);
                    log.info("JOB_CREATED - Saved discovered job ID [{}] - [{}] at [{}]", savedJob.getId(), savedJob.getTitle(), company.getName());
                }

                searchRun.setJobsDiscoveredCount(discoveredInRun);
                searchRun.setJobsIngestedCount(ingestedInRun);
                searchRun.setStatus("COMPLETED");
                searchRun.setCompletedAt(Instant.now());
                searchRunRepository.save(searchRun);

            } catch (Exception e) {
                log.error("DISCOVERY_FAILED for query [{}]: {}", query.getQueryString(), e.getMessage());
                summary.getErrors().add("Query '" + query.getQueryString() + "' failed: " + e.getMessage());
                searchRun.setStatus("FAILED");
                searchRun.setErrorMessage(e.getMessage());
                searchRun.setCompletedAt(Instant.now());
                searchRunRepository.save(searchRun);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        summary.setExecutionTimeMs(duration);

        // Record Agent Run
        AgentRun agentRun = new AgentRun(
                "DiscoveryAgent",
                summary.getErrors().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS",
                duration
        );
        if (!summary.getErrors().isEmpty()) {
            agentRun.setErrorMessage(String.join("; ", summary.getErrors()));
        }
        agentRunRepository.save(agentRun);

        summary.setJobs(newlyCreatedJobs.stream().map(this::mapToDto).toList());

        log.info("DISCOVERY_COMPLETED - Found: [{}], Scraped: [{}], Ingested: [{}], Duplicates: [{}], Invalid: [{}], Duration: [{}ms]",
                summary.getSearchResultsFound(), summary.getPagesScraped(), summary.getJobsCreated(),
                summary.getDuplicatesSkipped(), summary.getInvalidPagesSkipped(), duration);

        return summary;
    }

    public JobDto mapToDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        if (job.getCompany() != null) {
            dto.setCompanyName(job.getCompany().getName());
            dto.setCompanyDomain(job.getCompany().getDomain());
        }
        dto.setTitle(job.getTitle());
        dto.setNormalizedTitle(job.getNormalizedTitle());
        dto.setDepartment(job.getDepartment());
        dto.setLocation(job.getLocation());
        dto.setWorkMode(job.getWorkMode());
        dto.setEmploymentType(job.getEmploymentType());
        dto.setMinExperienceYears(job.getMinExperienceYears());
        dto.setMaxExperienceYears(job.getMaxExperienceYears());
        dto.setMinSalary(job.getMinSalary());
        dto.setMaxSalary(job.getMaxSalary());
        dto.setSalaryCurrency(job.getSalaryCurrency());
        dto.setJobUrl(job.getJobUrl());
        dto.setCanonicalUrl(job.getCanonicalUrl());
        dto.setRawDescriptionMarkdown(job.getRawDescriptionMarkdown());
        dto.setStructuredJobSpec(job.getStructuredJobSpec());
        dto.setPostingDate(job.getPostingDate());
        dto.setDeadlineDate(job.getDeadlineDate());
        dto.setActive(job.isActive());
        dto.setPipelineStatus(job.getPipelineStatus());
        if (job.getJobSource() != null) {
            dto.setSourceName(job.getJobSource().getName());
        }
        dto.setCreatedAt(job.getCreatedAt());

        // Extract detected technologies from structuredJobSpec JSON
        try {
            if (job.getStructuredJobSpec() != null && !job.getStructuredJobSpec().isBlank()) {
                Map<String, Object> spec = objectMapper.readValue(job.getStructuredJobSpec(), new TypeReference<>() {});
                if (spec.containsKey("detectedTechnologies")) {
                    Object tech = spec.get("detectedTechnologies");
                    if (tech instanceof List<?>) {
                        List<String> list = ((List<?>) tech).stream().map(Object::toString).toList();
                        dto.setDetectedTechnologies(list);
                    }
                }
            }
        } catch (Exception ignored) {}

        return dto;
    }

    private JobSource resolveJobSource(String sourceIdentifier) {
        String sourceName = "FIRECRAWL_" + sourceIdentifier;
        return jobSourceRepository.findByName(sourceName)
                .orElseGet(() -> jobSourceRepository.findByName("FIRECRAWL_GREENHOUSE")
                        .orElseGet(() -> {
                            JobSource js = new JobSource(sourceName, "FIRECRAWL_SEARCH", "https://api.firecrawl.dev", 10);
                            return jobSourceRepository.save(js);
                        }));
    }
}
