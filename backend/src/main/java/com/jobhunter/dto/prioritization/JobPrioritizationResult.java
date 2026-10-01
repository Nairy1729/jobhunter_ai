package com.jobhunter.dto.prioritization;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobPrioritizationResult {

    private UUID jobId;
    private UUID candidateProfileId;
    private PriorityCategory priorityCategory;
    private Integer priorityScore = 0; // Internal deterministic ranking score 0-100 (NOT hiring probability)
    private JobFreshness freshness;
    private Integer daysSincePosted;
    private List<String> whyThisJob = new ArrayList<>();
    private List<String> potentialConcerns = new ArrayList<>();
    private List<String> hardConstraintViolations = new ArrayList<>();
    private List<RequirementCoverageItem> requirementCoverage = new ArrayList<>();
    private List<KeyTechCoverageDto> keyTechnologies = new ArrayList<>();
    private Instant evaluatedAt = Instant.now();

    public JobPrioritizationResult() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public UUID getCandidateProfileId() { return candidateProfileId; }
    public void setCandidateProfileId(UUID candidateProfileId) { this.candidateProfileId = candidateProfileId; }

    public PriorityCategory getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(PriorityCategory priorityCategory) { this.priorityCategory = priorityCategory; }

    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }

    public JobFreshness getFreshness() { return freshness; }
    public void setFreshness(JobFreshness freshness) { this.freshness = freshness; }

    public Integer getDaysSincePosted() { return daysSincePosted; }
    public void setDaysSincePosted(Integer daysSincePosted) { this.daysSincePosted = daysSincePosted; }

    public List<String> getWhyThisJob() { return whyThisJob; }
    public void setWhyThisJob(List<String> whyThisJob) { this.whyThisJob = whyThisJob; }

    public List<String> getPotentialConcerns() { return potentialConcerns; }
    public void setPotentialConcerns(List<String> potentialConcerns) { this.potentialConcerns = potentialConcerns; }

    public List<String> getHardConstraintViolations() { return hardConstraintViolations; }
    public void setHardConstraintViolations(List<String> hardConstraintViolations) { this.hardConstraintViolations = hardConstraintViolations; }

    public List<RequirementCoverageItem> getRequirementCoverage() { return requirementCoverage; }
    public void setRequirementCoverage(List<RequirementCoverageItem> requirementCoverage) { this.requirementCoverage = requirementCoverage; }

    public List<KeyTechCoverageDto> getKeyTechnologies() { return keyTechnologies; }
    public void setKeyTechnologies(List<KeyTechCoverageDto> keyTechnologies) { this.keyTechnologies = keyTechnologies; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
