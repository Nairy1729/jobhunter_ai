package com.jobhunter.service.matching;

import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.dto.matching.UsefulnessDecision;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Job;
import com.jobhunter.service.discovery.HardEligibilityFilterService;
import com.jobhunter.service.discovery.dto.HardEligibilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The Usefulness Gate.
 * Enforces the core product rule:
 * "Is this job sufficiently aligned with this candidate that showing it is likely to be useful?"
 *
 * Categorizes opportunities into:
 * - HIGH_RELEVANCE
 * - GOOD_MATCH
 * - POSSIBLE_MATCH
 * - NOT_USEFUL (never displayed to user)
 */
@Service
public class UsefulnessGateService {

    private static final Logger log = LoggerFactory.getLogger(UsefulnessGateService.class);

    private final HardEligibilityFilterService hardEligibilityFilter;

    public UsefulnessGateService(HardEligibilityFilterService hardEligibilityFilter) {
        this.hardEligibilityFilter = hardEligibilityFilter;
    }

    public UsefulnessDecision evaluateUsefulness(Job job, CandidateProfile profile, MatchAnalysisResponse match) {
        // 1. Hard Eligibility Filter Pre-check
        HardEligibilityResult hardCheck = hardEligibilityFilter.evaluate(job, profile);
        if (!hardCheck.isEligible()) {
            UsefulnessDecision decision = new UsefulnessDecision(
                    false,
                    "NOT_USEFUL",
                    "NOT_USEFUL",
                    "Disqualified by Hard Eligibility Filter: " + hardCheck.getReason()
            );
            return decision;
        }

        if (match == null) {
            // Default conservative decision if semantic match is not yet persisted
            return new UsefulnessDecision(false, "NOT_USEFUL", "NOT_USEFUL", "Pending semantic match evaluation.");
        }

        double score = match.getPriorityScore() != null ? match.getPriorityScore().doubleValue() : 0.0;
        List<String> strongMatches = match.getStrongMatches() != null ? match.getStrongMatches() : List.of();
        List<String> partialMatches = match.getPartialMatches() != null ? match.getPartialMatches() : List.of();
        List<String> gaps = match.getGaps() != null ? match.getGaps() : List.of();
        List<String> evidence = match.getSupportingEvidence() != null ? match.getSupportingEvidence() : List.of();

        String recommendation = match.getRecommendation() != null ? match.getRecommendation() : "DO_NOT_APPLY";

        // 2. Strict Filter: DO_NOT_APPLY or zero verified alignment
        if ("DO_NOT_APPLY".equalsIgnoreCase(recommendation) || (strongMatches.isEmpty() && partialMatches.isEmpty())) {
            log.info("USEFULNESS_GATE_DROP - Job [{}] dropped: DO_NOT_APPLY or zero verified overlap (Score: {})", job.getId(), score);
            UsefulnessDecision decision = new UsefulnessDecision(
                    false,
                    "NOT_USEFUL",
                    "NOT_USEFUL",
                    "Role does not have sufficient verified alignment with candidate profile."
            );
            decision.setMissingRequirements(gaps);
            return decision;
        }

        // 3. Category Assignment
        String category;
        String status;
        String rationale;

        if (score >= 68.0 && strongMatches.size() >= 2) {
            category = "HIGH_RELEVANCE";
            status = "USEFUL";
            rationale = "Strong alignment with candidate verified skills, commercial tenure, and target responsibilities.";
        } else if (score >= 48.0 && !strongMatches.isEmpty()) {
            category = "GOOD_MATCH";
            status = "USEFUL";
            rationale = "Strong technical core alignment with only minor gaps in optional or preferred qualifications.";
        } else if (score >= 38.0 && (!strongMatches.isEmpty() || partialMatches.size() >= 2)) {
            category = "POSSIBLE_MATCH";
            status = "USEFUL";
            rationale = "Relevant role and experience level, with several areas requiring targeted resume framing.";
        } else {
            category = "NOT_USEFUL";
            status = "NOT_USEFUL";
            rationale = "Insufficient relevance score (" + score + ") to meet the usefulness threshold.";
        }

        boolean isUseful = "USEFUL".equals(status);
        UsefulnessDecision decision = new UsefulnessDecision(isUseful, status, category, rationale);

        List<String> matched = new ArrayList<>(strongMatches);
        matched.addAll(partialMatches);
        decision.setMatchedRequirements(matched);
        decision.setMissingRequirements(gaps);
        decision.setCandidateEvidence(evidence);

        return decision;
    }
}
