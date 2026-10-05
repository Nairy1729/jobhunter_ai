package com.jobhunter.security;

import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Resume;
import com.jobhunter.model.entity.User;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.ResumeRepository;
import com.jobhunter.service.ResumeParserService;
import com.jobhunter.service.discovery.UrlNormalizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SecurityAuditRegressionTest {

    private UrlNormalizer urlNormalizer;
    private AuthRateLimitingFilter authRateLimitingFilter;

    @BeforeEach
    void setUp() {
        urlNormalizer = new UrlNormalizer();
        authRateLimitingFilter = new AuthRateLimitingFilter();
    }

    // =========================================================================
    // 1. SSRF & EXTERNAL URL VALIDATION AUDIT
    // =========================================================================

    @Test
    @DisplayName("STRIX-SSRF: Blocks localhost, loopback, and private IPv4/IPv6 address spaces")
    void testSsrfProtection_BlocksPrivateAndLocalAddresses() {
        assertFalse(urlNormalizer.isValidPublicUrl("http://localhost:8080/admin"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://localhost/"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://127.0.0.1:8085/api/actuator"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://127.0.0.1/"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://169.254.169.254/latest/meta-data")); // AWS/Cloud metadata
        assertFalse(urlNormalizer.isValidPublicUrl("http://10.0.0.5/internal"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://192.168.1.1/router"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://172.16.0.1/dashboard"));
        assertFalse(urlNormalizer.isValidPublicUrl("http://0.0.0.0:80/"));
        assertFalse(urlNormalizer.isValidPublicUrl("file:///etc/passwd"));
        assertFalse(urlNormalizer.isValidPublicUrl("javascript:alert(1)"));
        assertFalse(urlNormalizer.isValidPublicUrl("ftp://internal.vault/secrets"));
    }

    @Test
    @DisplayName("STRIX-SSRF: Allows verified public domains (.gov.in, .nic.in, official portals)")
    void testSsrfProtection_AllowsLegitimatePublicDomains() {
        assertTrue(urlNormalizer.isValidPublicUrl("https://upsc.gov.in/recruitment/notice"));
        assertTrue(urlNormalizer.isValidPublicUrl("https://delhi.gov.in/vacancies"));
        assertTrue(urlNormalizer.isValidPublicUrl("https://lucknow.nic.in/recruitment/samvida.pdf"));
        assertTrue(urlNormalizer.isValidPublicUrl("https://careers.google.com/jobs/results"));
    }

    // =========================================================================
    // 2. IDOR / BROKEN OBJECT LEVEL AUTHORIZATION (RESUME OWNERSHIP)
    // =========================================================================

    @Test
    @DisplayName("STRIX-IDOR: Rejects User B accessing User A's Master Resume")
    void testIdor_RejectsUnauthorizedResumeAccess() {
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();
        UUID resumeId = UUID.randomUUID();

        User userA = new User("userA@jobhunter.ai", "hashA", "Alice", "Smith");
        userA.setId(userAId);

        CandidateProfile profileA = new CandidateProfile();
        profileA.setId(UUID.randomUUID());
        profileA.setUser(userA);

        Resume resumeA = new Resume();
        resumeA.setId(resumeId);
        resumeA.setCandidateProfile(profileA);

        ResumeRepository resumeRepository = mock(ResumeRepository.class);
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resumeA));

        CandidateProfileRepository profileRepository = mock(CandidateProfileRepository.class);
        ResumeParserService parserService = mock(ResumeParserService.class);

        com.jobhunter.controller.ResumeController resumeController =
                new com.jobhunter.controller.ResumeController(parserService, resumeRepository, profileRepository);

        User userB = new User("userB@jobhunter.ai", "hashB", "Bob", "Jones");
        userB.setId(userBId);
        CustomUserDetails userBPrincipal = new CustomUserDetails(userB);

        // Attempt IDOR: User B requests User A's resume
        assertThrows(SecurityException.class, () -> {
            resumeController.getResume(userBPrincipal, resumeId);
        });

        // User A requests their own resume: Succeeds
        CustomUserDetails userAPrincipal = new CustomUserDetails(userA);
        var response = resumeController.getResume(userAPrincipal, resumeId);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    // =========================================================================
    // 3. FILE UPLOAD & PATH TRAVERSAL DEFENSE
    // =========================================================================

    @Test
    @DisplayName("STRIX-UPLOAD: Rejects non-allowlisted executable files (.sh, .exe, .jsp)")
    void testResumeUpload_RejectsExecutableFiles() throws IOException {
        Path tempUploadDir = Files.createTempDirectory("sec_upload_test_");

        CandidateProfileRepository profileRepository = mock(CandidateProfileRepository.class);
        CandidateProfile profile = new CandidateProfile();
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ResumeParserService parserService = new ResumeParserService(
                mock(ResumeRepository.class),
                profileRepository,
                mock(com.jobhunter.repository.SkillRepository.class),
                mock(com.jobhunter.service.ai.ResumeAiOrchestratorService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                tempUploadDir.toString()
        );

        MockMultipartFile executableUpload = new MockMultipartFile(
                "file",
                "malicious.sh",
                "text/x-sh",
                "#!/bin/bash\nrm -rf /".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            parserService.uploadAndParseMasterResume(userId, executableUpload);
        });
    }

    @Test
    @DisplayName("STRIX-UPLOAD: Rejects forged PDF missing valid magic bytes")
    void testResumeUpload_RejectsForgedPdfHeader() throws IOException {
        Path tempUploadDir = Files.createTempDirectory("sec_upload_test_2_");

        CandidateProfileRepository profileRepository = mock(CandidateProfileRepository.class);
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(new CandidateProfile()));

        ResumeParserService parserService = new ResumeParserService(
                mock(ResumeRepository.class),
                profileRepository,
                mock(com.jobhunter.repository.SkillRepository.class),
                mock(com.jobhunter.service.ai.ResumeAiOrchestratorService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                tempUploadDir.toString()
        );

        // Name says .pdf, but bytes are plain text/fake
        MockMultipartFile forgedPdf = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "NOT A REAL PDF DOCUMENT AT ALL".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            parserService.uploadAndParseMasterResume(userId, forgedPdf);
        });
    }

    // =========================================================================
    // 4. RATE LIMITING DEFENSE (BRUTE FORCE / CREDENTIAL STUFFING)
    // =========================================================================

    @Test
    @DisplayName("STRIX-RATE-LIMIT: Returns 429 Too Many Requests after threshold is exceeded")
    void testAuthRateLimitingFilter_BlocksAfter15Attempts() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);

        // Perform 15 allowed attempts
        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("203.0.113.195");
            MockHttpServletResponse response = new MockHttpServletResponse();

            authRateLimitingFilter.doFilter(request, response, filterChain);
            assertNotEquals(429, response.getStatus(), "Attempt " + i + " should not be blocked");
        }

        // 16th attempt from the same IP must be rejected with 429 Too Many Requests
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedRequest.setRemoteAddr("203.0.113.195");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        authRateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);
        assertEquals(429, blockedResponse.getStatus());
        assertEquals("60", blockedResponse.getHeader("Retry-After"));
        assertTrue(blockedResponse.getContentAsString().contains("Too many authentication attempts"));
    }
}
