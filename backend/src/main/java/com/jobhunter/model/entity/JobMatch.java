package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_matches",
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "candidate_profile_id"}))
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false, length = 50)
    private String recommendation; // APPLY, APPLY_AFTER_TAILORING, LOW_PRIORITY, DO_NOT_APPLY

    @Column(name = "priority_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal priorityScore = BigDecimal.ZERO;

    @Column(name = "queue_tier", nullable = false, length = 50)
    private String queueTier = "REVIEW"; // HIGH_PRIORITY, REVIEW, LOW_PRIORITY

    @Column(name = "strong_matches", columnDefinition = "jsonb", nullable = false)
    private String strongMatches = "[]";

    @Column(name = "partial_matches", columnDefinition = "jsonb", nullable = false)
    private String partialMatches = "[]";

    @Column(columnDefinition = "jsonb", nullable = false)
    private String gaps = "[]";

    @Column(name = "transferable_experience", columnDefinition = "jsonb", nullable = false)
    private String transferableExperience = "[]";

    @Column(name = "risk_factors", columnDefinition = "jsonb", nullable = false)
    private String riskFactors = "[]";

    @Column(name = "advantage_report", columnDefinition = "jsonb", nullable = false)
    private String advantageReport = "{}";

    @Column(name = "priority_category", length = 50)
    private String priorityCategory;

    @Column(name = "freshness", length = 20)
    private String freshness;

    @Column(name = "days_since_posted")
    private Integer daysSincePosted;

    @Column(name = "why_this_job", columnDefinition = "jsonb")
    private String whyThisJob = "[]";

    @Column(name = "potential_concerns", columnDefinition = "jsonb")
    private String potentialConcerns = "[]";

    @Column(name = "requirement_coverage", columnDefinition = "jsonb")
    private String requirementCoverage = "[]";

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    public JobMatch() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public BigDecimal getPriorityScore() { return priorityScore; }
    public void setPriorityScore(BigDecimal priorityScore) { this.priorityScore = priorityScore; }

    public String getQueueTier() { return queueTier; }
    public void setQueueTier(String queueTier) { this.queueTier = queueTier; }

    public String getStrongMatches() { return strongMatches; }
    public void setStrongMatches(String strongMatches) { this.strongMatches = strongMatches; }

    public String getPartialMatches() { return partialMatches; }
    public void setPartialMatches(String partialMatches) { this.partialMatches = partialMatches; }

    public String getGaps() { return gaps; }
    public void setGaps(String gaps) { this.gaps = gaps; }

    public String getTransferableExperience() { return transferableExperience; }
    public void setTransferableExperience(String transferableExperience) { this.transferableExperience = transferableExperience; }

    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }

    public String getAdvantageReport() { return advantageReport; }
    public void setAdvantageReport(String advantageReport) { this.advantageReport = advantageReport; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(String priorityCategory) { this.priorityCategory = priorityCategory; }

    public String getFreshness() { return freshness; }
    public void setFreshness(String freshness) { this.freshness = freshness; }

    public Integer getDaysSincePosted() { return daysSincePosted; }
    public void setDaysSincePosted(Integer daysSincePosted) { this.daysSincePosted = daysSincePosted; }

    public String getWhyThisJob() { return whyThisJob; }
    public void setWhyThisJob(String whyThisJob) { this.whyThisJob = whyThisJob; }

    public String getPotentialConcerns() { return potentialConcerns; }
    public void setPotentialConcerns(String potentialConcerns) { this.potentialConcerns = potentialConcerns; }

    public String getRequirementCoverage() { return requirementCoverage; }
    public void setRequirementCoverage(String requirementCoverage) { this.requirementCoverage = requirementCoverage; }
}
