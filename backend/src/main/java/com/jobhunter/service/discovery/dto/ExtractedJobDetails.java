package com.jobhunter.service.discovery.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExtractedJobDetails {

    private String title;
    private String normalizedTitle;
    private String companyName;
    private String location;
    private String workMode = "UNKNOWN";
    private String employmentType = "FULL_TIME";
    private BigDecimal minExperienceYears;
    private BigDecimal maxExperienceYears;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryCurrency;
    private String rawDescriptionMarkdown;
    private List<String> responsibilities = new ArrayList<>();
    private List<String> requiredQualifications = new ArrayList<>();
    private List<String> preferredQualifications = new ArrayList<>();
    private List<String> detectedTechnologies = new ArrayList<>();
    private LocalDate postingDate;
    private LocalDate deadlineDate;
    private String jobUrl;
    private String canonicalUrl;
    private String canonicalUrlHash;
    private String contentHash;

    public ExtractedJobDetails() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNormalizedTitle() { return normalizedTitle; }
    public void setNormalizedTitle(String normalizedTitle) { this.normalizedTitle = normalizedTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

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

    public String getRawDescriptionMarkdown() { return rawDescriptionMarkdown; }
    public void setRawDescriptionMarkdown(String rawDescriptionMarkdown) { this.rawDescriptionMarkdown = rawDescriptionMarkdown; }

    public List<String> getResponsibilities() { return responsibilities; }
    public void setResponsibilities(List<String> responsibilities) { this.responsibilities = responsibilities; }

    public List<String> getRequiredQualifications() { return requiredQualifications; }
    public void setRequiredQualifications(List<String> requiredQualifications) { this.requiredQualifications = requiredQualifications; }

    public List<String> getPreferredQualifications() { return preferredQualifications; }
    public void setPreferredQualifications(List<String> preferredQualifications) { this.preferredQualifications = preferredQualifications; }

    public List<String> getDetectedTechnologies() { return detectedTechnologies; }
    public void setDetectedTechnologies(List<String> detectedTechnologies) { this.detectedTechnologies = detectedTechnologies; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public String getCanonicalUrlHash() { return canonicalUrlHash; }
    public void setCanonicalUrlHash(String canonicalUrlHash) { this.canonicalUrlHash = canonicalUrlHash; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
