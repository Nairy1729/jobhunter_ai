package com.jobhunter.service.government;

import com.jobhunter.model.entity.government.*;
import com.jobhunter.repository.government.GovernmentJobCorrigendumRepository;
import com.jobhunter.repository.government.GovernmentJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class GovernmentDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(GovernmentDeduplicationService.class);

    private final GovernmentJobRepository jobRepository;
    private final GovernmentJobCorrigendumRepository corrigendumRepository;

    public GovernmentDeduplicationService(
            GovernmentJobRepository jobRepository,
            GovernmentJobCorrigendumRepository corrigendumRepository) {
        this.jobRepository = jobRepository;
        this.corrigendumRepository = corrigendumRepository;
    }

    /**
     * Generates a deterministic canonical ID from key identity tokens.
     */
    public String generateCanonicalId(String organization, String notificationNumber, String title, String state, String district) {
        String orgNorm = organization != null ? organization.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String notifNorm = notificationNumber != null && !notificationNumber.equalsIgnoreCase("NOT_SPECIFIED")
                ? notificationNumber.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String titleNorm = title != null ? title.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String stateNorm = state != null ? state.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String distNorm = district != null ? district.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

        String combined;
        if (!notifNorm.isEmpty() && !orgNorm.isEmpty()) {
            combined = "gov_" + orgNorm + "_" + notifNorm;
        } else {
            combined = "gov_" + orgNorm + "_" + distNorm + "_" + stateNorm + "_" + titleNorm;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 16; i++) { // 32 hex chars
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "gov-" + hexString;
        } catch (Exception e) {
            return "gov-" + Math.abs(combined.hashCode());
        }
    }

    /**
     * Finds if a canonical job already exists for this notification or title.
     */
    public Optional<GovernmentJob> findCanonicalJob(String canonicalId, String notificationNumber, String org) {
        Optional<GovernmentJob> byCanonical = jobRepository.findByCanonicalId(canonicalId);
        if (byCanonical.isPresent()) {
            return byCanonical;
        }

        if (notificationNumber != null && !notificationNumber.isBlank() && !notificationNumber.equalsIgnoreCase("NOT_SPECIFIED")) {
            List<GovernmentJob> byNotif = jobRepository.findByNotificationNumberIgnoreCase(notificationNumber.trim());
            if (!byNotif.isEmpty()) {
                return Optional.of(byNotif.get(0));
            }
        }

        return Optional.empty();
    }

    /**
     * Attaches a corrigendum or revision to an existing job.
     */
    @Transactional
    public GovernmentJob applyCorrigendum(GovernmentJob existingJob, GovernmentJobCorrigendum corrigendum) {
        log.info("Applying corrigendum [{}] to existing canonical job [{}]", corrigendum.getTitle(), existingJob.getCanonicalId());

        corrigendum.setJob(existingJob);
        corrigendumRepository.save(corrigendum);

        if (corrigendum.getRevisedLastDate() != null) {
            existingJob.setApplicationLastDate(corrigendum.getRevisedLastDate());
            if (corrigendum.getRevisedLastDate().isAfter(Instant.now())) {
                existingJob.setStatus(GovernmentJobStatus.OPEN);
            }
        }

        if (corrigendum.getRevisedVacancies() != null && corrigendum.getRevisedVacancies() > 0) {
            existingJob.setVacanciesCount(corrigendum.getRevisedVacancies());
        }

        if (corrigendum.getNoticeType() == CorrigendumType.CANCELLATION) {
            existingJob.setStatus(GovernmentJobStatus.CANCELLED);
        } else if (corrigendum.getNoticeType() == CorrigendumType.POSTPONEMENT) {
            existingJob.setStatus(GovernmentJobStatus.POSTPONED);
        }

        existingJob.setUpdatedAt(Instant.now());
        return jobRepository.save(existingJob);
    }

    /**
     * Evaluates and updates expiry states across jobs (Section 24).
     */
    @Transactional
    public int refreshExpiryStatus() {
        Instant now = Instant.now();
        Instant closingSoonThreshold = now.plus(3, ChronoUnit.DAYS);

        List<GovernmentJob> expiredJobs = jobRepository.findByStatusAndApplicationLastDateBefore(GovernmentJobStatus.OPEN, now);
        for (GovernmentJob job : expiredJobs) {
            job.setStatus(GovernmentJobStatus.CLOSED);
            jobRepository.save(job);
        }

        return expiredJobs.size();
    }
}
