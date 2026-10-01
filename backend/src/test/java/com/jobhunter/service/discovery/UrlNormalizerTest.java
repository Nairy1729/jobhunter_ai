package com.jobhunter.service.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlNormalizerTest {

    private UrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new UrlNormalizer();
    }

    @Test
    @DisplayName("Should accept valid public HTTP and HTTPS URLs")
    void shouldAcceptValidPublicUrls() {
        assertTrue(normalizer.isValidPublicUrl("https://boards.greenhouse.io/stripe/jobs/12345"));
        assertTrue(normalizer.isValidPublicUrl("https://jobs.lever.co/netflix/abc-123"));
        assertTrue(normalizer.isValidPublicUrl("https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite/job/Austin-TX/Software-Engineer_JR1982"));
        assertTrue(normalizer.isValidPublicUrl("http://company.com/careers/posting"));
    }

    @Test
    @DisplayName("Should reject SSRF and internal network targets")
    void shouldRejectSsrfAndInternalTargets() {
        assertFalse(normalizer.isValidPublicUrl("http://localhost:8080/admin"));
        assertFalse(normalizer.isValidPublicUrl("http://127.0.0.1/status"));
        assertFalse(normalizer.isValidPublicUrl("http://10.0.0.1/secret"));
        assertFalse(normalizer.isValidPublicUrl("http://192.168.1.100/config"));
        assertFalse(normalizer.isValidPublicUrl("http://169.254.169.254/latest/meta-data/"));
        assertFalse(normalizer.isValidPublicUrl("http://metadata.google.internal/computeMetadata/v1/"));
        assertFalse(normalizer.isValidPublicUrl("http://service.local/"));
        assertFalse(normalizer.isValidPublicUrl("ftp://company.com/jobs"));
        assertFalse(normalizer.isValidPublicUrl("file:///etc/passwd"));
        assertFalse(normalizer.isValidPublicUrl("javascript:alert(1)"));
        assertFalse(normalizer.isValidPublicUrl(""));
        assertFalse(normalizer.isValidPublicUrl(null));
    }

    @Test
    @DisplayName("Should normalize URLs by removing tracking parameters and sorting query params")
    void shouldNormalizeUrlsAndStripTrackingParams() {
        String raw = "https://boards.greenhouse.io/stripe/jobs/12345?gh_src=partner_link&utm_source=linkedin&utm_medium=cpc&ref=job_board#apply";
        String normalized = normalizer.normalizeUrl(raw);
        assertEquals("https://boards.greenhouse.io/stripe/jobs/12345", normalized);

        String withKeptParams = "https://example.com/job/view?role=engineer&utm_source=twitter&category=backend";
        String normalizedWithParams = normalizer.normalizeUrl(withKeptParams);
        assertEquals("https://example.com/job/view?category=backend&role=engineer", normalizedWithParams);
    }

    @Test
    @DisplayName("Should generate deterministic SHA-256 hashes")
    void shouldGenerateDeterministicSha256() {
        String hash1 = UrlNormalizer.computeSha256("https://boards.greenhouse.io/stripe/jobs/12345");
        String hash2 = UrlNormalizer.computeSha256("https://boards.greenhouse.io/stripe/jobs/12345");
        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);

        String hash3 = UrlNormalizer.computeSha256("https://boards.greenhouse.io/stripe/jobs/99999");
        assertNotEquals(hash1, hash3);
    }
}
