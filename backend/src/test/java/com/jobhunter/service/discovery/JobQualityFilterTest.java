package com.jobhunter.service.discovery;

import com.jobhunter.service.discovery.adapter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobQualityFilterTest {

    private JobQualityFilter filter;

    @BeforeEach
    void setUp() {
        UrlNormalizer normalizer = new UrlNormalizer();
        GreenhouseAdapter gh = new GreenhouseAdapter(normalizer);
        LeverAdapter lever = new LeverAdapter(normalizer);
        WorkdayAdapter wd = new WorkdayAdapter(normalizer);
        AshbyAdapter ashby = new AshbyAdapter(normalizer);
        GenericCareerAdapter generic = new GenericCareerAdapter(normalizer);
        JobSourceAdapterRegistry registry = new JobSourceAdapterRegistry(
                List.of(gh, lever, wd, ashby, generic),
                generic
        );
        filter = new JobQualityFilter(normalizer, registry);
    }

    @Test
    @DisplayName("Should accept promising job URLs and reject non-job URLs")
    void testIsPromisingUrl() {
        assertTrue(filter.isPromisingUrl("https://boards.greenhouse.io/stripe/jobs/123456", "Software Engineer"));
        assertTrue(filter.isPromisingUrl("https://jobs.lever.co/netflix/3fa85f64-5717-4562-b3fc-2c963f66afa6", "Backend Engineer"));
        assertTrue(filter.isPromisingUrl("https://adobe.wd5.myworkdayjobs.com/job/Seattle-WA/Developer_JR100", "Developer"));

        // Reject homepages and directory pages
        assertFalse(filter.isPromisingUrl("https://boards.greenhouse.io/stripe", "Careers at Stripe"));
        assertFalse(filter.isPromisingUrl("https://company.com/careers", "Careers"));
        assertFalse(filter.isPromisingUrl("https://company.com/jobs/", "All Jobs"));
        assertFalse(filter.isPromisingUrl("http://localhost:8080/job/123", "Internal"));
    }

    @Test
    @DisplayName("Should reject expired or closed job listings")
    void testRejectExpiredJobPages() {
        String expiredMarkdown = "# Software Engineer\n\nThis job posting has expired and is no longer accepting applications. Thank you for your interest.";
        assertFalse(filter.isQualityJobPage("https://boards.greenhouse.io/stripe/jobs/123", "Software Engineer", expiredMarkdown, 200));

        String closedMarkdown = "# Backend Developer\n\nThis position has been filled.";
        assertFalse(filter.isQualityJobPage("https://jobs.lever.co/netflix/123", "Backend Developer", closedMarkdown, 200));

        assertFalse(filter.isQualityJobPage("https://example.com/job/1", "Job", "Valid markdown but page not found", 404));
        assertFalse(filter.isQualityJobPage("https://example.com/job/1", "Job", "Valid markdown but gone", 410));
    }

    @Test
    @DisplayName("Should reject pages with insufficient or irrelevant content")
    void testRejectInsufficientContent() {
        assertFalse(filter.isQualityJobPage("https://example.com/job/1", "Job", "Too short", 200));
        assertFalse(filter.isQualityJobPage("https://example.com/job/1", "404 Not Found", "Some error happened on the site.", 200));
    }

    @Test
    @DisplayName("Should accept valid, high-quality job postings")
    void testAcceptQualityJobPage() {
        String validMarkdown = """
                # Senior Backend Engineer
                
                **Location**: Bangalore, India | Hybrid
                
                ## Responsibilities
                - Build scalable distributed services using Java 17 and Spring Boot.
                - Design relational database schemas in PostgreSQL.
                
                ## Requirements
                - 2+ years of experience in backend development.
                - Strong proficiency in Java, REST APIs, and SQL.
                """;

        assertTrue(filter.isQualityJobPage("https://boards.greenhouse.io/stripe/jobs/12345", "Senior Backend Engineer", validMarkdown, 200));
    }
}
