package com.jobhunter.model.entity.government;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "government_jobs")
public class GovernmentJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "canonical_id", nullable = false, unique = true, length = 255)
    private String canonicalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 255)
    private String organization;

    @Column(length = 255)
    private String department;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String block;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 60)
    private GovernmentEmploymentType employmentType = GovernmentEmploymentType.REGULAR;

    @Column(name = "vacancies_count")
    private Integer vacanciesCount;

    @Column(name = "vacancies_breakdown_json", columnDefinition = "TEXT")
    private String vacanciesBreakdownJson = "[]";

    @Column(length = 255)
    private String salary;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "pay_level", length = 100)
    private String payLevel;

    @Column(nullable = false)
    private boolean honorarium = false;

    @Column(name = "application_mode", nullable = false, length = 60)
    private String applicationMode = "ONLINE";

    @Column(name = "application_fee", length = 255)
    private String applicationFee;

    @Column(name = "application_start_date")
    private Instant applicationStartDate;

    @Column(name = "application_last_date")
    private Instant applicationLastDate;

    @Column(name = "exam_date")
    private Instant examDate;

    @Column(name = "interview_date")
    private Instant interviewDate;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "notification_url", length = 1000)
    private String notificationUrl;

    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Column(length = 255)
    private String authority;

    @Column(name = "source_domain", length = 255)
    private String sourceDomain;

    @Column(name = "notification_number", length = 255)
    private String notificationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 60)
    private GovernmentVerificationStatus verificationStatus = GovernmentVerificationStatus.VERIFIED_OFFICIAL;

    @Column(name = "authenticity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal authenticityScore = BigDecimal.valueOf(95.0);

    @Enumerated(EnumType.STRING)
    @Column(name = "authenticity_level", nullable = false, length = 40)
    private AuthenticityLevel authenticityLevel = AuthenticityLevel.VERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GovernmentJobStatus status = GovernmentJobStatus.OPEN;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private GovernmentSource source;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private GovernmentJobEligibility eligibility;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GovernmentJobEvidence> evidenceList = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GovernmentJobCorrigendum> corrigenda = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public GovernmentJob() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCanonicalId() { return canonicalId; }
    public void setCanonicalId(String canonicalId) { this.canonicalId = canonicalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public GovernmentEmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(GovernmentEmploymentType employmentType) { this.employmentType = employmentType; }

    public Integer getVacanciesCount() { return vacanciesCount; }
    public void setVacanciesCount(Integer vacanciesCount) { this.vacanciesCount = vacanciesCount; }

    public String getVacanciesBreakdownJson() { return vacanciesBreakdownJson; }
    public void setVacanciesBreakdownJson(String vacanciesBreakdownJson) { this.vacanciesBreakdownJson = vacanciesBreakdownJson; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }

    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }

    public String getPayLevel() { return payLevel; }
    public void setPayLevel(String payLevel) { this.payLevel = payLevel; }

    public boolean isHonorarium() { return honorarium; }
    public void setHonorarium(boolean honorarium) { this.honorarium = honorarium; }

    public String getApplicationMode() { return applicationMode; }
    public void setApplicationMode(String applicationMode) { this.applicationMode = applicationMode; }

    public String getApplicationFee() { return applicationFee; }
    public void setApplicationFee(String applicationFee) { this.applicationFee = applicationFee; }

    public Instant getApplicationStartDate() { return applicationStartDate; }
    public void setApplicationStartDate(Instant applicationStartDate) { this.applicationStartDate = applicationStartDate; }

    public Instant getApplicationLastDate() { return applicationLastDate; }
    public void setApplicationLastDate(Instant applicationLastDate) { this.applicationLastDate = applicationLastDate; }

    public Instant getExamDate() { return examDate; }
    public void setExamDate(Instant examDate) { this.examDate = examDate; }

    public Instant getInterviewDate() { return interviewDate; }
    public void setInterviewDate(Instant interviewDate) { this.interviewDate = interviewDate; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getNotificationUrl() { return notificationUrl; }
    public void setNotificationUrl(String notificationUrl) { this.notificationUrl = notificationUrl; }

    public String getApplicationUrl() { return applicationUrl; }
    public void setApplicationUrl(String applicationUrl) { this.applicationUrl = applicationUrl; }

    public String getAuthority() { return authority; }
    public void setAuthority(String authority) { this.authority = authority; }

    public String getSourceDomain() { return sourceDomain; }
    public void setSourceDomain(String sourceDomain) { this.sourceDomain = sourceDomain; }

    public String getNotificationNumber() { return notificationNumber; }
    public void setNotificationNumber(String notificationNumber) { this.notificationNumber = notificationNumber; }

    public GovernmentVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(GovernmentVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public BigDecimal getAuthenticityScore() { return authenticityScore; }
    public void setAuthenticityScore(BigDecimal authenticityScore) { this.authenticityScore = authenticityScore; }

    public AuthenticityLevel getAuthenticityLevel() { return authenticityLevel; }
    public void setAuthenticityLevel(AuthenticityLevel authenticityLevel) { this.authenticityLevel = authenticityLevel; }

    public GovernmentJobStatus getStatus() { return status; }
    public void setStatus(GovernmentJobStatus status) { this.status = status; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public GovernmentSource getSource() { return source; }
    public void setSource(GovernmentSource source) { this.source = source; }

    public GovernmentJobEligibility getEligibility() { return eligibility; }
    public void setEligibility(GovernmentJobEligibility eligibility) {
        this.eligibility = eligibility;
        if (eligibility != null) {
            eligibility.setJob(this);
        }
    }

    public List<GovernmentJobEvidence> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<GovernmentJobEvidence> evidenceList) { this.evidenceList = evidenceList; }

    public List<GovernmentJobCorrigendum> getCorrigenda() { return corrigenda; }
    public void setCorrigenda(List<GovernmentJobCorrigendum> corrigenda) { this.corrigenda = corrigenda; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
