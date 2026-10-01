package com.jobhunter.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobDto {

    private UUID id;
    private String companyName;
    private String companyDomain;
    private String title;
    private String normalizedTitle;
    private String department;
    private String location;
    private String workMode;
    private String employmentType;
    private BigDecimal minExperienceYears;
    private BigDecimal maxExperienceYears;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryCurrency;
    private String jobUrl;
    private String canonicalUrl;
    private String rawDescriptionMarkdown;
    private String structuredJobSpec;
    private List<String> detectedTechnologies = new ArrayList<>();
    private LocalDate postingDate;
    private LocalDate deadlineDate;
    private boolean active;
    private String pipelineStatus;
    private String sourceName;
    private Instant createdAt;
    private boolean applied;
    private Instant appliedAt;
    private String priorityCategory;
    private Integer priorityScore;
    private String freshness;
    private Integer daysSincePosted;
    private List<String> whyThisJob = new ArrayList<>();
    private List<String> potentialConcerns = new ArrayList<>();
    private List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> keyTechnologies = new ArrayList<>();
    private String usefulnessStatus = "USEFUL";
    private String matchCategory;
    private List<String> matchedRequirements = new ArrayList<>();
    private List<String> missingRequirements = new ArrayList<>();
    private List<String> candidateEvidence = new ArrayList<>();
    private Boolean hardEligibilityPassed = true;

    public JobDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyDomain() { return companyDomain; }
    public void setCompanyDomain(String companyDomain) { this.companyDomain = companyDomain; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNormalizedTitle() { return normalizedTitle; }
    public void setNormalizedTitle(String normalizedTitle) { this.normalizedTitle = normalizedTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public BigDecimal getMinExperienceYears() { return minExperienceYears; }
    public void setMinExperienceYears(BigDecimal minExperienceYears) { this.minExperienceYears = minExperienceYears; }

    public BigDecimal getMaxExperienceYears() { return maxExperienceYears; }
    public void setMaxExperienceYears(BigDecimal maxExperienceYears) { this.maxExperienceYears = maxExperienceYears; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getSalaryCurrency() { return salaryCurrency; }
    public void setSalaryCurrency(String salaryCurrency) { this.salaryCurrency = salaryCurrency; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public String getRawDescriptionMarkdown() { return rawDescriptionMarkdown; }
    public void setRawDescriptionMarkdown(String rawDescriptionMarkdown) { this.rawDescriptionMarkdown = rawDescriptionMarkdown; }

    public String getStructuredJobSpec() { return structuredJobSpec; }
    public void setStructuredJobSpec(String structuredJobSpec) { this.structuredJobSpec = structuredJobSpec; }

    public List<String> getDetectedTechnologies() { return detectedTechnologies; }
    public void setDetectedTechnologies(List<String> detectedTechnologies) { this.detectedTechnologies = detectedTechnologies; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getPipelineStatus() { return pipelineStatus; }
    public void setPipelineStatus(String pipelineStatus) { this.pipelineStatus = pipelineStatus; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }

    public String getPriorityCategory() { return priorityCategory; }
    public void setPriorityCategory(String priorityCategory) { this.priorityCategory = priorityCategory; }

    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }

    public String getFreshness() { return freshness; }
    public void setFreshness(String freshness) { this.freshness = freshness; }

    public Integer getDaysSincePosted() { return daysSincePosted; }
    public void setDaysSincePosted(Integer daysSincePosted) { this.daysSincePosted = daysSincePosted; }

    public List<String> getWhyThisJob() { return whyThisJob; }
    public void setWhyThisJob(List<String> whyThisJob) { this.whyThisJob = whyThisJob; }

    public List<String> getPotentialConcerns() { return potentialConcerns; }
    public void setPotentialConcerns(List<String> potentialConcerns) { this.potentialConcerns = potentialConcerns; }

    public List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> getKeyTechnologies() { return keyTechnologies; }
    public void setKeyTechnologies(List<com.jobhunter.dto.prioritization.KeyTechCoverageDto> keyTechnologies) { this.keyTechnologies = keyTechnologies; }

    public String getUsefulnessStatus() { return usefulnessStatus; }
    public void setUsefulnessStatus(String usefulnessStatus) { this.usefulnessStatus = usefulnessStatus; }

    public String getMatchCategory() { return matchCategory; }
    public void setMatchCategory(String matchCategory) { this.matchCategory = matchCategory; }

    public List<String> getMatchedRequirements() { return matchedRequirements; }
    public void setMatchedRequirements(List<String> matchedRequirements) { this.matchedRequirements = matchedRequirements; }

    public List<String> getMissingRequirements() { return missingRequirements; }
    public void setMissingRequirements(List<String> missingRequirements) { this.missingRequirements = missingRequirements; }

    public List<String> getCandidateEvidence() { return candidateEvidence; }
    public void setCandidateEvidence(List<String> candidateEvidence) { this.candidateEvidence = candidateEvidence; }

    public Boolean getHardEligibilityPassed() { return hardEligibilityPassed; }
    public void setHardEligibilityPassed(Boolean hardEligibilityPassed) { this.hardEligibilityPassed = hardEligibilityPassed; }
}
