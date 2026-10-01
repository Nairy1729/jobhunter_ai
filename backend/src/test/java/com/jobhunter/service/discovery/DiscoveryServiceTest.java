package com.jobhunter.service.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.client.firecrawl.FirecrawlClient;
import com.jobhunter.client.firecrawl.dto.*;
import com.jobhunter.model.dto.DiscoveryRequest;
import com.jobhunter.model.dto.DiscoverySummaryDto;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.discovery.adapter.*;
import com.jobhunter.service.discovery.dto.GeneratedSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {

    private CandidateProfileRepository candidateProfileRepository;
    private JobRepository jobRepository;
    private CompanyRepository companyRepository;
    private JobSourceRepository jobSourceRepository;
    private SearchRunRepository searchRunRepository;
    private AgentRunRepository agentRunRepository;
    private FirecrawlClient firecrawlClient;
    private JobSearchQueryGenerator queryGenerator;
    private UrlNormalizer urlNormalizer;
    private JobSourceAdapterRegistry adapterRegistry;
    private JobQualityFilter jobQualityFilter;
    private DeduplicationService deduplicationService;
    private JobExtractorService jobExtractorService;
    private ObjectMapper objectMapper;

    private DiscoveryService discoveryService;

    private UUID userId;
    private CandidateProfile candidateProfile;
    private JobSource defaultJobSource;

    @BeforeEach
    void setUp() {
        candidateProfileRepository = mock(CandidateProfileRepository.class);
        jobRepository = mock(JobRepository.class);
        companyRepository = mock(CompanyRepository.class);
        jobSourceRepository = mock(JobSourceRepository.class);
        searchRunRepository = mock(SearchRunRepository.class);
        agentRunRepository = mock(AgentRunRepository.class);
        firecrawlClient = mock(FirecrawlClient.class);
        queryGenerator = mock(JobSearchQueryGenerator.class);
        objectMapper = new ObjectMapper();

        urlNormalizer = new UrlNormalizer();
        GreenhouseAdapter gh = new GreenhouseAdapter(urlNormalizer);
        LeverAdapter lever = new LeverAdapter(urlNormalizer);
        WorkdayAdapter wd = new WorkdayAdapter(urlNormalizer);
        AshbyAdapter ashby = new AshbyAdapter(urlNormalizer);
        GenericCareerAdapter generic = new GenericCareerAdapter(urlNormalizer);
        adapterRegistry = new JobSourceAdapterRegistry(List.of(gh, lever, wd, ashby, generic), generic);

        jobQualityFilter = new JobQualityFilter(urlNormalizer, adapterRegistry);
        deduplicationService = new DeduplicationService(jobRepository);

        SkillRepository skillRepository = mock(SkillRepository.class);
        when(skillRepository.findAll()).thenReturn(List.of(new Skill("Java", "LANGUAGE", "[]"), new Skill("Spring Boot", "FRAMEWORK", "[]")));
        jobExtractorService = new JobExtractorService(skillRepository, deduplicationService);

        com.jobhunter.service.profile.ProfileReadinessService profileReadinessService = mock(com.jobhunter.service.profile.ProfileReadinessService.class);
        com.jobhunter.dto.profile.ProfileReadinessReport readyReport = new com.jobhunter.dto.profile.ProfileReadinessReport(
                com.jobhunter.dto.profile.ProfileReadinessState.PROFILE_READY,
                100,
                true,
                "PROFILE READY",
                "Ready for discovery"
        );
        when(profileReadinessService.evaluateProfile(any())).thenReturn(readyReport);

        discoveryService = new DiscoveryService(
                candidateProfileRepository,
                jobRepository,
                companyRepository,
                jobSourceRepository,
                searchRunRepository,
                agentRunRepository,
                firecrawlClient,
                queryGenerator,
                urlNormalizer,
                adapterRegistry,
                jobQualityFilter,
                deduplicationService,
                jobExtractorService,
                profileReadinessService,
                objectMapper
        );

        userId = UUID.randomUUID();
        candidateProfile = new CandidateProfile();
        candidateProfile.setId(UUID.randomUUID());
        candidateProfile.setTargetRoles("[\"Backend Engineer\"]");
        candidateProfile.setPreferredLocations("[\"Bangalore\"]");

        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(candidateProfile));

        defaultJobSource = new JobSource("FIRECRAWL_GREENHOUSE", "FIRECRAWL_SEARCH", "https://boards.greenhouse.io", 10);
        when(jobSourceRepository.findByName(anyString())).thenReturn(Optional.of(defaultJobSource));
        when(searchRunRepository.save(any(SearchRun.class))).thenAnswer(i -> {
            SearchRun sr = i.getArgument(0);
            sr.setId(UUID.randomUUID());
            return sr;
        });
        when(companyRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job j = i.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });
        when(firecrawlClient.isAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("Should execute complete discovery pipeline and save fresh jobs")
    void shouldExecuteDiscoveryPipeline() {
        GeneratedSearchQuery query = new GeneratedSearchQuery(
                "site:boards.greenhouse.io (\"Java\" OR \"Spring Boot\") (\"Backend Engineer\")",
                "GREENHOUSE",
                List.of("Backend Engineer"),
                List.of("Java", "Spring Boot"),
                List.of("Bangalore")
        );
        when(queryGenerator.generateQueries(eq(candidateProfile), anyInt())).thenReturn(List.of(query));

        // 1. Search returns 2 results: 1 good job, 1 generic careers page
        FirecrawlDocument doc1 = new FirecrawlDocument(
                "https://boards.greenhouse.io/stripe/jobs/112233?gh_src=custom",
                "Backend Engineer - Stripe",
                "# Backend Engineer\n\n## Requirements\n- Java and Spring Boot experience."
        );
        FirecrawlDocument doc2 = new FirecrawlDocument(
                "https://boards.greenhouse.io/stripe",
                "Stripe Careers",
                "Generic listing page"
        );
        FirecrawlSearchResponse searchRes = new FirecrawlSearchResponse(true, List.of(doc1, doc2));
        when(firecrawlClient.search(any())).thenReturn(searchRes);

        // 2. Scrape doc1 returns full markdown
        FirecrawlDocument scrapedDoc1 = new FirecrawlDocument(
                "https://boards.greenhouse.io/stripe/jobs/112233",
                "Backend Engineer - Stripe",
                """
                # Backend Engineer
                
                **Location**: Bangalore, India
                
                ## Responsibilities
                - Build payment infrastructure using Java and Spring Boot.
                
                ## Requirements
                - 2+ years of experience with Spring Boot.
                """
        );
        FirecrawlScrapeResponse scrapeRes = new FirecrawlScrapeResponse(true, scrapedDoc1);
        when(firecrawlClient.scrape(any())).thenReturn(scrapeRes);

        when(jobRepository.existsByCanonicalUrlHash(anyString())).thenReturn(false);
        when(jobRepository.existsByContentHash(anyString())).thenReturn(false);

        DiscoveryRequest req = new DiscoveryRequest(1, 10);
        DiscoverySummaryDto summary = discoveryService.discoverJobs(userId, req);

        assertNotNull(summary);
        assertEquals(1, summary.getQueriesExecuted());
        assertEquals(2, summary.getSearchResultsFound());
        assertEquals(1, summary.getPagesScraped());
        assertEquals(1, summary.getJobsCreated());
        assertEquals(1, summary.getInvalidPagesSkipped()); // doc2 rejected
        assertEquals(0, summary.getDuplicatesSkipped());
        assertEquals(1, summary.getJobs().size());

        verify(jobRepository, times(1)).save(any(Job.class));
        verify(searchRunRepository, times(2)).save(any(SearchRun.class)); // 1 start, 1 completion
        verify(agentRunRepository, times(1)).save(any(AgentRun.class));
    }

    @Test
    @DisplayName("Tier 1 Deduplication: Should skip scraping known canonical URLs")
    void shouldSkipKnownCanonicalUrls() {
        GeneratedSearchQuery query = new GeneratedSearchQuery("site:boards.greenhouse.io \"Java\"", "GREENHOUSE", List.of("Engineer"), List.of("Java"), List.of());
        when(queryGenerator.generateQueries(any(), anyInt())).thenReturn(List.of(query));

        FirecrawlDocument doc = new FirecrawlDocument("https://boards.greenhouse.io/stripe/jobs/12345", "Backend Engineer", "Job body");
        when(firecrawlClient.search(any())).thenReturn(new FirecrawlSearchResponse(true, List.of(doc)));

        // Canonical URL already exists in database
        when(jobRepository.existsByCanonicalUrlHash(anyString())).thenReturn(true);

        DiscoverySummaryDto summary = discoveryService.discoverJobs(userId, new DiscoveryRequest(1, 10));

        assertEquals(1, summary.getDuplicatesSkipped());
        assertEquals(0, summary.getPagesScraped());
        assertEquals(0, summary.getJobsCreated());
        verify(firecrawlClient, never()).scrape(any());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("Tier 2 Deduplication: Should scrape but drop cross-posted duplicate content")
    void shouldDropCrossPostedDuplicateContent() {
        GeneratedSearchQuery query = new GeneratedSearchQuery("site:boards.greenhouse.io \"Java\"", "GREENHOUSE", List.of("Engineer"), List.of("Java"), List.of());
        when(queryGenerator.generateQueries(any(), anyInt())).thenReturn(List.of(query));

        FirecrawlDocument doc = new FirecrawlDocument("https://boards.greenhouse.io/stripe/jobs/998877", "Backend Engineer", "Job body");
        when(firecrawlClient.search(any())).thenReturn(new FirecrawlSearchResponse(true, List.of(doc)));

        // Canonical URL is new
        when(jobRepository.existsByCanonicalUrlHash(anyString())).thenReturn(false);

        // Scrape succeeds
        FirecrawlDocument scraped = new FirecrawlDocument(
                "https://boards.greenhouse.io/stripe/jobs/998877",
                "Backend Engineer",
                "# Backend Engineer\n\n## Responsibilities\n- Design systems with Java.\n\n## Requirements\n- 2+ years of experience."
        );
        when(firecrawlClient.scrape(any())).thenReturn(new FirecrawlScrapeResponse(true, scraped));

        // Content hash matches existing cross-posted job
        when(jobRepository.existsByContentHash(anyString())).thenReturn(true);

        DiscoverySummaryDto summary = discoveryService.discoverJobs(userId, new DiscoveryRequest(1, 10));

        assertEquals(1, summary.getPagesScraped());
        assertEquals(1, summary.getDuplicatesSkipped());
        assertEquals(0, summary.getJobsCreated());
        verify(jobRepository, never()).save(any(Job.class));
    }
}
