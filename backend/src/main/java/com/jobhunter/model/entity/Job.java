package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_source_id", nullable = false)
    private JobSource jobSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_run_id")
    private SearchRun searchRun;

    @Column(nullable = false)
    private String title;

    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    @Column(length = 100)
    private String department;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "work_mode", nullable = false, length = 50)
    private String workMode = "UNKNOWN"; // REMOTE, HYBRID, ON_SITE, UNKNOWN

    @Column(name = "employment_type", length = 50)
    private String employmentType = "FULL_TIME";

    @Column(name = "min_experience_years", precision = 3, scale = 1)
    private BigDecimal minExperienceYears;

    @Column(name = "max_experience_years", precision = 3, scale = 1)
    private BigDecimal maxExperienceYears;

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Column(name = "salary_currency", length = 10)
    private String salaryCurrency;

    @Column(name = "job_url", nullable = false, length = 1000)
    private String jobUrl;

    @Column(name = "canonical_url", nullable = false, length = 1000)
    private String canonicalUrl;

    @Column(name = "canonical_url_hash", nullable = false, unique = true, length = 64)
    private String canonicalUrlHash;

    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(name = "raw_description_markdown", columnDefinition = "TEXT", nullable = false)
    private String rawDescriptionMarkdown;

    @Column(name = "structured_job_spec", columnDefinition = "jsonb", nullable = false)
    private String structuredJobSpec = "{}";

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "pipeline_status", nullable = false, length = 50)
    private String pipelineStatus = "DISCOVERED"; // DISCOVERED, ANALYZED, SHORTLISTED, FILTERED_OUT

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Job() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public JobSource getJobSource() { return jobSource; }
    public void setJobSource(JobSource jobSource) { this.jobSource = jobSource; }

    public SearchRun getSearchRun() { return searchRun; }
    public void setSearchRun(SearchRun searchRun) { this.searchRun = searchRun; }

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

    public String getCanonicalUrlHash() { return canonicalUrlHash; }
    public void setCanonicalUrlHash(String canonicalUrlHash) { this.canonicalUrlHash = canonicalUrlHash; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getRawDescriptionMarkdown() { return rawDescriptionMarkdown; }
    public void setRawDescriptionMarkdown(String rawDescriptionMarkdown) { this.rawDescriptionMarkdown = rawDescriptionMarkdown; }

    public String getStructuredJobSpec() { return structuredJobSpec; }
    public void setStructuredJobSpec(String structuredJobSpec) { this.structuredJobSpec = structuredJobSpec; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getPipelineStatus() { return pipelineStatus; }
    public void setPipelineStatus(String pipelineStatus) { this.pipelineStatus = pipelineStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
