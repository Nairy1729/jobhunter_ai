package com.jobhunter.service.discovery;

import com.jobhunter.model.entity.Job;
import com.jobhunter.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DeduplicationServiceTest {

    private JobRepository jobRepository;
    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
        deduplicationService = new DeduplicationService(jobRepository);
    }

    @Test
    @DisplayName("Tier 1: Canonical URL hash deduplication check")
    void testTier1CanonicalUrlHash() {
        String canonicalUrl = "https://boards.greenhouse.io/stripe/jobs/12345";
        String hash = deduplicationService.generateCanonicalUrlHash(canonicalUrl);

        assertNotNull(hash);
        assertEquals(64, hash.length());

        when(jobRepository.existsByCanonicalUrlHash(hash)).thenReturn(true);
        assertTrue(deduplicationService.isDuplicateCanonicalUrl(hash));

        when(jobRepository.existsByCanonicalUrlHash(hash)).thenReturn(false);
        assertFalse(deduplicationService.isDuplicateCanonicalUrl(hash));
    }

    @Test
    @DisplayName("Tier 2: Content identity hash detects cross-posted jobs")
    void testTier2ContentHash() {
        String title = "Backend Engineer - Payments";
        String company = "Stripe";
        String body = "We are seeking a Backend Engineer with 2+ years of Java and Spring Boot experience to scale our payment processing engine.";

        String contentHash1 = deduplicationService.generateContentHash(title, company, body);
        assertNotNull(contentHash1);
        assertEquals(64, contentHash1.length());

        // Same job content with minor whitespace/case differences
        String bodySlightlyDifferentCase = "we are seeking a backend engineer with 2+ years of java and spring boot experience to scale our payment processing engine.   ";
        String contentHash2 = deduplicationService.generateContentHash("Backend Engineer - Payments", "Stripe", bodySlightlyDifferentCase);

        assertEquals(contentHash1, contentHash2);

        when(jobRepository.existsByContentHash(contentHash1)).thenReturn(true);
        assertTrue(deduplicationService.isDuplicateContent(contentHash1));
    }

    @Test
    @DisplayName("Tier 2: Different jobs produce distinct content hashes")
    void testTier2DistinctContentHashes() {
        String hash1 = deduplicationService.generateContentHash("Software Engineer", "Google", "Search backend engineer");
        String hash2 = deduplicationService.generateContentHash("Data Engineer", "Google", "Search data engineer");

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Tier 3: Same company, normalized title, location and high description similarity is detected as duplicate opportunity")
    void testTier3DuplicateOpportunity_SameLocationAndContent() {
        Job existing = new Job();
        existing.setActive(true);
        existing.setCompany(new com.jobhunter.model.entity.Company("Acme Corp"));
        existing.setTitle("Backend Engineer");
        existing.setNormalizedTitle("backend engineer");
        existing.setLocation("Bangalore");
        existing.setRawDescriptionMarkdown("We are seeking a Backend Engineer with Java, Spring Boot, and PostgreSQL experience to build high-scale APIs.");

        when(jobRepository.findAll()).thenReturn(java.util.List.of(existing));

        boolean isDup = deduplicationService.isDuplicateOpportunity(
                "Acme Corp",
                "Backend Engineer",
                "Bangalore",
                "We are seeking a Backend Engineer with Java, Spring Boot, and PostgreSQL experience to build high-scale APIs."
        );

        assertTrue(isDup, "Should detect duplicate opportunity across sources");
    }

    @Test
    @DisplayName("Tier 3: Different locations for the same company and title must remain distinct opportunities")
    void testTier3DuplicateOpportunity_DifferentLocationPreserved() {
        Job existing = new Job();
        existing.setActive(true);
        existing.setCompany(new com.jobhunter.model.entity.Company("Acme Corp"));
        existing.setTitle("Backend Engineer");
        existing.setNormalizedTitle("backend engineer");
        existing.setLocation("Bangalore");
        existing.setRawDescriptionMarkdown("We are seeking a Backend Engineer with Java and Spring Boot experience.");

        when(jobRepository.findAll()).thenReturn(java.util.List.of(existing));

        // Same company, same title, but in San Francisco (different location)
        boolean isDup = deduplicationService.isDuplicateOpportunity(
                "Acme Corp",
                "Backend Engineer",
                "San Francisco",
                "We are seeking a Backend Engineer with Java and Spring Boot experience."
        );

        assertFalse(isDup, "Different office locations must remain distinct opportunities");
    }
}
