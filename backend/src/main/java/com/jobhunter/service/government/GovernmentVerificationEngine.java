package com.jobhunter.service.government;

import com.jobhunter.model.entity.government.AuthenticityLevel;
import com.jobhunter.model.entity.government.GovernmentJob;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Set;

@Service
public class GovernmentVerificationEngine {

    // Known official Indian government top-level domains & patterns
    private static final Set<String> OFFICIAL_TLDS = Set.of(
            ".gov.in",
            ".nic.in",
            ".ac.in",
            ".edu.in",
            ".res.in",
            ".org.in"
    );

    public static class VerificationAssessment {
        private final BigDecimal score;
        private final AuthenticityLevel level;
        private final GovernmentVerificationStatus status;

        public VerificationAssessment(BigDecimal score, AuthenticityLevel level, GovernmentVerificationStatus status) {
            this.score = score;
            this.level = level;
            this.status = status;
        }

        public BigDecimal getScore() { return score; }
        public AuthenticityLevel getLevel() { return level; }
        public GovernmentVerificationStatus getStatus() { return status; }
    }

    public VerificationAssessment assessAuthenticity(GovernmentJob job) {
        if (job == null) {
            return new VerificationAssessment(BigDecimal.ZERO, AuthenticityLevel.UNVERIFIED, GovernmentVerificationStatus.UNVERIFIED);
        }

        // Check expiry first
        if (job.getApplicationLastDate() != null && job.getApplicationLastDate().isBefore(Instant.now())) {
            return new VerificationAssessment(
                    BigDecimal.valueOf(50.0),
                    AuthenticityLevel.PARTIALLY_VERIFIED,
                    GovernmentVerificationStatus.EXPIRED
            );
        }

        int score = 0;
        boolean hasOfficialDomain = isOfficialGovernmentDomain(job.getSourceDomain()) ||
                                   isOfficialGovernmentDomain(extractDomain(job.getSourceUrl())) ||
                                   isOfficialGovernmentDomain(extractDomain(job.getNotificationUrl()));

        if (hasOfficialDomain) {
            score += 40;
        }

        if (job.getNotificationUrl() != null && !job.getNotificationUrl().isBlank()) {
            score += 25;
            if (isOfficialGovernmentDomain(extractDomain(job.getNotificationUrl()))) {
                score += 5;
            }
        }

        if (job.getApplicationUrl() != null && !job.getApplicationUrl().isBlank()) {
            score += 15;
            if (isOfficialGovernmentDomain(extractDomain(job.getApplicationUrl()))) {
                score += 5;
            }
        }

        if (job.getNotificationNumber() != null && !job.getNotificationNumber().isBlank()
                && !job.getNotificationNumber().equalsIgnoreCase("NOT_SPECIFIED")) {
            score += 10;
        }

        if (job.getAuthority() != null && !job.getAuthority().isBlank()) {
            score += 10;
        }

        BigDecimal finalScore = BigDecimal.valueOf(Math.min(100, score));
        AuthenticityLevel level;
        GovernmentVerificationStatus status;

        if (hasOfficialDomain && score >= 70) {
            level = AuthenticityLevel.VERIFIED;
            status = GovernmentVerificationStatus.VERIFIED_OFFICIAL;
        } else if (job.getNotificationUrl() != null && score >= 50) {
            level = AuthenticityLevel.PARTIALLY_VERIFIED;
            status = GovernmentVerificationStatus.VERIFIED_OFFICIAL_NOTIFICATION;
        } else {
            level = AuthenticityLevel.UNVERIFIED;
            status = GovernmentVerificationStatus.UNVERIFIED;
        }

        return new VerificationAssessment(finalScore, level, status);
    }

    public boolean isOfficialGovernmentDomain(String domain) {
        if (domain == null || domain.isBlank()) return false;
        String lower = domain.toLowerCase().trim();

        for (String tld : OFFICIAL_TLDS) {
            if (lower.endsWith(tld) || lower.contains(tld + "/")) {
                return true;
            }
        }

        // PSU, AIIMS, and State Corporation domains
        if (lower.contains("upsc.gov.in") ||
            lower.contains("ssc.gov.in") ||
            lower.contains("indianrailways.gov.in") ||
            lower.contains("ncs.gov.in") ||
            lower.contains("employmentnews.gov.in") ||
            lower.contains("ibps.in") ||
            lower.contains("drdo.gov.in") ||
            lower.contains("isro.gov.in") ||
            lower.contains("aiims.edu") ||
            lower.contains("nhm.gov.in") ||
            lower.contains(".nic.in")) {
            return true;
        }

        return false;
    }

    public String extractDomain(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : "";
        } catch (Exception e) {
            // fallback simple string parsing
            String clean = url.replaceFirst("^(https?://)?(www\\.)?", "");
            int slashIndex = clean.indexOf('/');
            return slashIndex > 0 ? clean.substring(0, slashIndex).toLowerCase() : clean.toLowerCase();
        }
    }
}
