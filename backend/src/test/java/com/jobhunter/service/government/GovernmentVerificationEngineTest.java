package com.jobhunter.service.government;

import com.jobhunter.model.entity.government.AuthenticityLevel;
import com.jobhunter.model.entity.government.GovernmentJob;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class GovernmentVerificationEngineTest {

    private GovernmentVerificationEngine verificationEngine;

    @BeforeEach
    void setUp() {
        verificationEngine = new GovernmentVerificationEngine();
    }

    @Test
    @DisplayName("Official nic.in domain with notification and application link gets VERIFIED_OFFICIAL")
    void testOfficialJobVerification() {
        GovernmentJob job = new GovernmentJob();
        job.setSourceDomain("kannauj.nic.in");
        job.setSourceUrl("https://kannauj.nic.in/recruitment");
        job.setNotificationUrl("https://kannauj.nic.in/notices/advt_04_2026.pdf");
        job.setApplicationUrl("https://balvikasup.gov.in");
        job.setNotificationNumber("Advt. 04/2026");
        job.setAuthority("District Administration, Kannauj");
        job.setApplicationLastDate(Instant.now().plus(20, ChronoUnit.DAYS));

        GovernmentVerificationEngine.VerificationAssessment assessment = verificationEngine.assessAuthenticity(job);

        assertEquals(GovernmentVerificationStatus.VERIFIED_OFFICIAL, assessment.getStatus());
        assertEquals(AuthenticityLevel.VERIFIED, assessment.getLevel());
        assertTrue(assessment.getScore().doubleValue() >= 80.0);
    }

    @Test
    @DisplayName("Third-party job portal without official domain or notification gets UNVERIFIED")
    void testUnverifiedJob() {
        GovernmentJob job = new GovernmentJob();
        job.setSourceDomain("random-job-portal.com");
        job.setSourceUrl("https://random-job-portal.com/job/123");
        job.setNotificationUrl(null);
        job.setApplicationUrl(null);
        job.setNotificationNumber("NOT_SPECIFIED");
        job.setApplicationLastDate(Instant.now().plus(20, ChronoUnit.DAYS));

        GovernmentVerificationEngine.VerificationAssessment assessment = verificationEngine.assessAuthenticity(job);

        assertEquals(GovernmentVerificationStatus.UNVERIFIED, assessment.getStatus());
        assertEquals(AuthenticityLevel.UNVERIFIED, assessment.getLevel());
    }

    @Test
    @DisplayName("Past deadline jobs are marked as EXPIRED")
    void testExpiredJob() {
        GovernmentJob job = new GovernmentJob();
        job.setSourceDomain("upsc.gov.in");
        job.setNotificationUrl("https://upsc.gov.in/notices/exam.pdf");
        job.setApplicationLastDate(Instant.now().minus(5, ChronoUnit.DAYS));

        GovernmentVerificationEngine.VerificationAssessment assessment = verificationEngine.assessAuthenticity(job);

        assertEquals(GovernmentVerificationStatus.EXPIRED, assessment.getStatus());
    }
}
