package com.jobhunter.service.discovery;

import com.jobhunter.model.entity.Job;
import com.jobhunter.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    private final JobRepository jobRepository;

    public DeduplicationService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Tier 1: Generate SHA-256 hash of canonical URL
     */
    public String generateCanonicalUrlHash(String canonicalUrl) {
        if (canonicalUrl == null || canonicalUrl.isBlank()) {
            return "";
        }
        return UrlNormalizer.computeSha256(canonicalUrl.trim().toLowerCase());
    }

    /**
     * Tier 1 Check: Verify if canonical URL hash already exists in PostgreSQL
     */
    public boolean isDuplicateCanonicalUrl(String canonicalUrlHash) {
        if (canonicalUrlHash == null || canonicalUrlHash.isBlank()) {
            return false;
        }
        boolean exists = jobRepository.existsByCanonicalUrlHash(canonicalUrlHash);
        if (exists) {
            log.info("JOB_DUPLICATE_URL - Dropping known canonical URL hash: [{}]", canonicalUrlHash);
        }
        return exists;
    }

    /**
     * Tier 2: Generate SHA-256 content identity from title + company + clean body
     */
    public String generateContentHash(String title, String company, String body) {
        String cleanTitle = sanitize(title);
        String cleanCompany = sanitize(company);
        String cleanBody = sanitize(body);

        // Take up to first 1000 chars of normalized body for cross-posting match
        if (cleanBody.length() > 1000) {
            cleanBody = cleanBody.substring(0, 1000);
        }

        String fingerprint = cleanTitle + "|" + cleanCompany + "|" + cleanBody;
        return UrlNormalizer.computeSha256(fingerprint);
    }

    /**
     * Tier 2 Check: Verify if content hash already exists in PostgreSQL
     */
    public boolean isDuplicateContent(String contentHash) {
        if (contentHash == null || contentHash.isBlank()) {
            return false;
        }
        boolean exists = jobRepository.existsByContentHash(contentHash);
        if (exists) {
            log.info("JOB_DUPLICATE_CONTENT - Dropping cross-posted duplicate content hash: [{}]", contentHash);
        }
        return exists;
    }

    public Optional<Job> findExistingJobByContentHash(String contentHash) {
        return jobRepository.findByContentHash(contentHash);
    }

    /**
     * Tier 3 Check: Verify cross-source opportunity duplication
     * If company matches, title matches (normalized), and location matches,
     * check token-based Jaccard text similarity (>75%).
     * Crucially: different locations (e.g. Bangalore vs Hyderabad) remain distinct opportunities!
     */
    public boolean isDuplicateOpportunity(String company, String title, String location, String description) {
        if (company == null || title == null || company.isBlank() || title.isBlank()) {
            return false;
        }

        String normCompany = sanitize(company);
        String normTitle = sanitize(title);
        String normLocation = sanitize(location);

        java.util.List<Job> candidateJobs = jobRepository.findAll().stream()
                .filter(Job::isActive)
                .filter(j -> j.getCompany() != null && sanitize(j.getCompany().getName()).equals(normCompany))
                .filter(j -> sanitize(j.getTitle()).equals(normTitle) || sanitize(j.getNormalizedTitle()).equals(normTitle))
                .toList();

        for (Job existing : candidateJobs) {
            String existingLocation = sanitize(existing.getLocation());
            // Different location = distinct opportunity!
            boolean locationMatches = normLocation.isEmpty() || existingLocation.isEmpty()
                    || normLocation.equals(existingLocation)
                    || normLocation.contains(existingLocation)
                    || existingLocation.contains(normLocation);

            if (!locationMatches) {
                continue;
            }

            // High text similarity check (> 75%)
            double similarity = computeTextSimilarity(description, existing.getRawDescriptionMarkdown());
            if (similarity >= 0.75) {
                log.info("JOB_DUPLICATE_OPPORTUNITY - Found cross-source duplicate for company [{}], title [{}], location [{}] (similarity: {}%)",
                        company, title, location, Math.round(similarity * 100));
                return true;
            }
        }

        return false;
    }

    public double computeTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        String s1 = sanitize(text1);
        String s2 = sanitize(text2);
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        if (s1.equals(s2)) return 1.0;

        java.util.Set<String> words1 = new java.util.HashSet<>(java.util.Arrays.asList(s1.split("\\s+")));
        java.util.Set<String> words2 = new java.util.HashSet<>(java.util.Arrays.asList(s2.split("\\s+")));

        java.util.Set<String> intersection = new java.util.HashSet<>(words1);
        intersection.retainAll(words2);

        java.util.Set<String> union = new java.util.HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
