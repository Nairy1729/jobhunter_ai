package com.jobhunter.dto.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchAnalysisResponse {

    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private UUID candidateProfileId;
    private String recommendation; // APPLY, APPLY_AFTER_TAILORING, LOW_PRIORITY, DO_NOT_APPLY
    private BigDecimal priorityScore;
    private String queueTier; // HIGH_PRIORITY, REVIEW, LOW_PRIORITY
    private double semanticSimilarityScore;
    private String overallAssessment;
    private List<String> strongMatches = new ArrayList<>();
    private List<String> partialMatches = new ArrayList<>();
    private List<String> gaps = new ArrayList<>();
    private List<String> transferableExperience = new ArrayList<>();
    private List<String> riskFactors = new ArrayList<>();
    private List<String> supportingEvidence = new ArrayList<>();
    private List<RequirementMatchItem> requirementAnalysis = new ArrayList<>();
    private ApplicationAdvantageReportDto advantageReport;
    private String priorityCategory; // HIGH_PRIORITY, MEDIUM_PRIORITY, LOW_PRIORITY, NOT_RECOMMENDED
    private String freshness; // NEW, RECENT, OLDER, STALE, UNKNOWN
    private Integer daysSincePosted;
    private List<String> whyThisJob = new ArrayList<>();
    private List<String> potentialConcerns = new ArrayList<>();
    private List<String> hardConstraintViolations = new ArrayList<>();
    private List<com.jobhunter.dto.prioritization.RequirementCoverageItem> requirementCoverage = new ArrayList<>();
    private List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> keyTechnologies = new ArrayList<>();
    private Instant evaluatedAt;

    public MatchAnalysisResponse() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public UUID getCandidateProfileId() { return candidateProfileId; }
    public void setCandidateProfileId(UUID candidateProfileId) { this.candidateProfileId = candidateProfileId; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public BigDecimal getPriorityScore() { return priorityScore; }
    public void setPriorityScore(BigDecimal priorityScore) { this.priorityScore = priorityScore; }

    public String getQueueTier() { return queueTier; }
    public void setQueueTier(String queueTier) { this.queueTier = queueTier; }

    public double getSemanticSimilarityScore() { return semanticSimilarityScore; }
    public void setSemanticSimilarityScore(double semanticSimilarityScore) { this.semanticSimilarityScore = semanticSimilarityScore; }

    public String getOverallAssessment() { return overallAssessment; }
    public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }

    public List<String> getStrongMatches() { return strongMatches; }
    public void setStrongMatches(List<String> strongMatches) { this.strongMatches = strongMatches; }

    public List<String> getPartialMatches() { return partialMatches; }
    public void setPartialMatches(List<String> partialMatches) { this.partialMatches = partialMatches; }

    public List<String> getGaps() { return gaps; }
    public void setGaps(List<String> gaps) { this.gaps = gaps; }

    public List<String> getTransferableExperience() { return transferableExperience; }
    public void setTransferableExperience(List<String> transferableExperience) { this.transferableExperience = transferableExperience; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public List<String> getSupportingEvidence() { return supportingEvidence; }
    public void setSupportingEvidence(List<String> supportingEvidence) { this.supportingEvidence = supportingEvidence; }

    public List<RequirementMatchItem> getRequirementAnalysis() { return requirementAnalysis; }
    public void setRequirementAnalysis(List<RequirementMatchItem> requirementAnalysis) { this.requirementAnalysis = requirementAnalysis; }

    public ApplicationAdvantageReportDto getAdvantageReport() { return advantageReport; }
    public void setAdvantageReport(ApplicationAdvantageReportDto advantageReport) { this.advantageReport = advantageReport; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(String priorityCategory) { this.priorityCategory = priorityCategory; }

    public String getFreshness() { return freshness; }
    public void setFreshness(String freshness) { this.freshness = freshness; }

    public Integer getDaysSincePosted() { return daysSincePosted; }
    public void setDaysSincePosted(Integer daysSincePosted) { this.daysSincePosted = daysSincePosted; }

    public List<String> getWhyThisJob() { return whyThisJob; }
    public void setWhyThisJob(List<String> whyThisJob) { this.whyThisJob = whyThisJob; }

    public List<String> getPotentialConcerns() { return potentialConcerns; }
    public void setPotentialConcerns(List<String> potentialConcerns) { this.potentialConcerns = potentialConcerns; }

    public List<String> getHardConstraintViolations() { return hardConstraintViolations; }
    public void setHardConstraintViolations(List<String> hardConstraintViolations) { this.hardConstraintViolations = hardConstraintViolations; }

    public List<com.jobhunter.dto.prioritization.RequirementCoverageItem> getRequirementCoverage() { return requirementCoverage; }
    public void setRequirementCoverage(List<com.jobhunter.dto.prioritization.RequirementCoverageItem> requirementCoverage) { this.requirementCoverage = requirementCoverage; }

    public List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> getKeyTechnologies() { return keyTechnologies; }
    public void setKeyTechnologies(List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> keyTechnologies) { this.keyTechnologies = keyTechnologies; }
}
