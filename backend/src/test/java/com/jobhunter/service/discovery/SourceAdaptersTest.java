package com.jobhunter.service.discovery;

import com.jobhunter.service.discovery.adapter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceAdaptersTest {

    private UrlNormalizer urlNormalizer;
    private GreenhouseAdapter greenhouseAdapter;
    private LeverAdapter leverAdapter;
    private WorkdayAdapter workdayAdapter;
    private AshbyAdapter ashbyAdapter;
    private GenericCareerAdapter genericAdapter;
    private JobSourceAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        urlNormalizer = new UrlNormalizer();
        greenhouseAdapter = new GreenhouseAdapter(urlNormalizer);
        leverAdapter = new LeverAdapter(urlNormalizer);
        workdayAdapter = new WorkdayAdapter(urlNormalizer);
        ashbyAdapter = new AshbyAdapter(urlNormalizer);
        genericAdapter = new GenericCareerAdapter(urlNormalizer);
        registry = new JobSourceAdapterRegistry(
                List.of(greenhouseAdapter, leverAdapter, workdayAdapter, ashbyAdapter, genericAdapter),
                genericAdapter
        );
    }

    @Test
    @DisplayName("GreenhouseAdapter: should normalize tracking params, extract company, and detect posting status")
    void testGreenhouseAdapter() {
        String url1 = "https://boards.greenhouse.io/stripe/jobs/456789?gh_src=linkedin_ads&gh_jid=998877#apply";
        assertTrue(greenhouseAdapter.supports(url1));
        assertEquals("https://boards.greenhouse.io/stripe/jobs/456789", greenhouseAdapter.canonicalizeUrl(url1));
        assertEquals("Stripe", greenhouseAdapter.extractCompany(url1, "Backend Engineer - Stripe"));
        assertTrue(greenhouseAdapter.isJobPosting(url1, "Backend Engineer"));

        // Reject non-job greenhouse board listing
        String boardUrl = "https://boards.greenhouse.io/stripe";
        assertFalse(greenhouseAdapter.isJobPosting(boardUrl, "Current Openings at Stripe"));

        // Detect closed posting
        assertTrue(greenhouseAdapter.isJobPostingClosed("This job posting has expired and is no longer active", 200));
        assertTrue(greenhouseAdapter.isJobPostingClosed("", 404));
        assertFalse(greenhouseAdapter.isJobPostingClosed("We are hiring a passionate Backend Engineer", 200));
    }

    @Test
    @DisplayName("LeverAdapter: should normalize /apply anchor, extract company, and identify valid jobs")
    void testLeverAdapter() {
        String url = "https://jobs.lever.co/netflix/3fa85f64-5717-4562-b3fc-2c963f66afa6/apply?lever-source=indeed";
        assertTrue(leverAdapter.supports(url));
        assertEquals("https://jobs.lever.co/netflix/3fa85f64-5717-4562-b3fc-2c963f66afa6", leverAdapter.canonicalizeUrl(url));
        assertEquals("Netflix", leverAdapter.extractCompany(url, "Senior Software Engineer - Netflix"));
        assertTrue(leverAdapter.isJobPosting(url, "Senior Software Engineer"));

        // Reject root company page
        assertFalse(leverAdapter.isJobPosting("https://jobs.lever.co/netflix", "Jobs at Netflix"));

        // Detect closed posting
        assertTrue(leverAdapter.isJobPostingClosed("No longer accepting applications for this position", 200));
        assertTrue(leverAdapter.isJobPostingClosed("", 410));
    }

    @Test
    @DisplayName("WorkdayAdapter: should normalize workday URLs and extract tenant company")
    void testWorkdayAdapter() {
        String url = "https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite/job/Austin-TX/Senior-System-Software-Engineer_JR1982736?source=LinkedIn";
        assertTrue(workdayAdapter.supports(url));
        assertEquals("https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite/job/Austin-TX/Senior-System-Software-Engineer_JR1982736", workdayAdapter.canonicalizeUrl(url));
        assertEquals("Nvidia", workdayAdapter.extractCompany(url, "Senior System Software Engineer at Nvidia"));
        assertTrue(workdayAdapter.isJobPosting(url, "Senior System Software Engineer"));

        assertTrue(workdayAdapter.isJobPostingClosed("This position has been filled", 200));
    }

    @Test
    @DisplayName("GenericCareerAdapter: should act as fallback for custom career portals")
    void testGenericAdapter() {
        String url = "https://careers.airbnb.com/positions/software-engineer-infra/?utm_source=custom";
        assertTrue(genericAdapter.supports(url));
        assertEquals("https://careers.airbnb.com/positions/software-engineer-infra", genericAdapter.canonicalizeUrl(url));
        assertEquals("Airbnb", genericAdapter.extractCompany(url, "Software Engineer - Infrastructure at Airbnb"));
        assertTrue(genericAdapter.isJobPosting(url, "Software Engineer"));

        // Root homepage is not a job
        assertFalse(genericAdapter.isJobPosting("https://careers.airbnb.com/", "Careers at Airbnb"));
    }

    @Test
    @DisplayName("JobSourceAdapterRegistry: should route to specific adapter when supported and fallback otherwise")
    void testRegistryResolution() {
        assertEquals("GREENHOUSE", registry.findAdapter("https://boards.greenhouse.io/stripe/jobs/123").getSourceIdentifier());
        assertEquals("LEVER", registry.findAdapter("https://jobs.lever.co/figma/abc-123").getSourceIdentifier());
        assertEquals("WORKDAY", registry.findAdapter("https://adobe.wd5.myworkdayjobs.com/job/123").getSourceIdentifier());
        assertEquals("ASHBY", registry.findAdapter("https://jobs.ashbyhq.com/openai/abc-123").getSourceIdentifier());
        assertEquals("GENERIC", registry.findAdapter("https://careers.microsoft.com/us/en/job/123").getSourceIdentifier());
    }
}
