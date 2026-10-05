package com.jobhunter.model.dto.government;

import com.jobhunter.model.entity.government.AuthenticityLevel;
import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GovernmentJobDetailDto {
    private UUID id;
    private String canonicalId;
    private String title;
    private String organization;
    private String department;
    private String state;
    private String district;
    private String block;
    private GovernmentEmploymentType employmentType;
    private Integer vacanciesCount;
    private List<String> vacanciesBreakdown = new ArrayList<>();
    private String salary;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String payLevel;
    private boolean honorarium;
    private String applicationMode;
    private String applicationFee;
    private Instant applicationStartDate;
    private Instant applicationLastDate;
    private Instant examDate;
    private Instant interviewDate;
    private String sourceUrl;
    private String notificationUrl;
    private String applicationUrl;
    private String authority;
    private String sourceDomain;
    private String notificationNumber;
    private GovernmentVerificationStatus verificationStatus;
    private BigDecimal authenticityScore;
    private AuthenticityLevel authenticityLevel;
    private GovernmentJobStatus status;
    private String rawContent;

    // Eligibility specifics
    private String gender;
    private Integer minimumAge;
    private Integer maximumAge;
    private List<String> education = new ArrayList<>();
    private List<String> experience = new ArrayList<>();
    private BigDecimal experienceYearsMin;
    private String domicile;
    private List<String> categoryReservations = new ArrayList<>();
    private Boolean pwdEligible;
    private Boolean exServicemanEligible;
    private String otherConditions;

    // Transparency & Evidence
    public static class EvidenceItemDto {
        private String fieldName;
        private String fieldValue;
        private String sourceDocument;
        private String pageOrSection;
        private String excerpt;

        public EvidenceItemDto() {}
        public EvidenceItemDto(String fieldName, String fieldValue, String sourceDocument, String pageOrSection, String excerpt) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.sourceDocument = sourceDocument;
            this.pageOrSection = pageOrSection;
            this.excerpt = excerpt;
        }

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public String getFieldValue() { return fieldValue; }
        public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
        public String getSourceDocument() { return sourceDocument; }
        public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }
        public String getPageOrSection() { return pageOrSection; }
        public void setPageOrSection(String pageOrSection) { this.pageOrSection = pageOrSection; }
        public String getExcerpt() { return excerpt; }
        public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    }

    // Corrigenda item
    public static class CorrigendumItemDto {
        private String noticeType;
        private String title;
        private String documentUrl;
        private Instant issueDate;
        private String description;
        private Instant revisedLastDate;
        private Integer revisedVacancies;

        public CorrigendumItemDto() {}
        public CorrigendumItemDto(String noticeType, String title, String documentUrl, Instant issueDate, String description, Instant revisedLastDate, Integer revisedVacancies) {
            this.noticeType = noticeType;
            this.title = title;
            this.documentUrl = documentUrl;
            this.issueDate = issueDate;
            this.description = description;
            this.revisedLastDate = revisedLastDate;
            this.revisedVacancies = revisedVacancies;
        }

        public String getNoticeType() { return noticeType; }
        public void setNoticeType(String noticeType) { this.noticeType = noticeType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDocumentUrl() { return documentUrl; }
        public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
        public Instant getIssueDate() { return issueDate; }
        public void setIssueDate(Instant issueDate) { this.issueDate = issueDate; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Instant getRevisedLastDate() { return revisedLastDate; }
        public void setRevisedLastDate(Instant revisedLastDate) { this.revisedLastDate = revisedLastDate; }
        public Integer getRevisedVacancies() { return revisedVacancies; }
        public void setRevisedVacancies(Integer revisedVacancies) { this.revisedVacancies = revisedVacancies; }
    }

    private List<EvidenceItemDto> evidenceList = new ArrayList<>();
    private List<CorrigendumItemDto> corrigenda = new ArrayList<>();
    private EligibilityEvaluationResultDto candidateEligibility;
    private Instant createdAt;
    private Instant updatedAt;

    public GovernmentJobDetailDto() {}

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
    public List<String> getVacanciesBreakdown() { return vacanciesBreakdown; }
    public void setVacanciesBreakdown(List<String> vacanciesBreakdown) { this.vacanciesBreakdown = vacanciesBreakdown; }
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
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }
    public void setMaximumAge(Integer maximumAge) { this.maximumAge = maximumAge; }
    public List<String> getEducation() { return education; }
    public void setEducation(List<String> education) { this.education = education; }
    public List<String> getExperience() { return experience; }
    public void setExperience(List<String> experience) { this.experience = experience; }
    public BigDecimal getExperienceYearsMin() { return experienceYearsMin; }
    public void setExperienceYearsMin(BigDecimal experienceYearsMin) { this.experienceYearsMin = experienceYearsMin; }
    public String getDomicile() { return domicile; }
    public void setDomicile(String domicile) { this.domicile = domicile; }
    public List<String> getCategoryReservations() { return categoryReservations; }
    public void setCategoryReservations(List<String> categoryReservations) { this.categoryReservations = categoryReservations; }
    public Boolean getPwdEligible() { return pwdEligible; }
    public void setPwdEligible(Boolean pwdEligible) { this.pwdEligible = pwdEligible; }
    public Boolean getExServicemanEligible() { return exServicemanEligible; }
    public void setExServicemanEligible(Boolean exServicemanEligible) { this.exServicemanEligible = exServicemanEligible; }
    public String getOtherConditions() { return otherConditions; }
    public void setOtherConditions(String otherConditions) { this.otherConditions = otherConditions; }
    public List<EvidenceItemDto> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<EvidenceItemDto> evidenceList) { this.evidenceList = evidenceList; }
    public List<CorrigendumItemDto> getCorrigenda() { return corrigenda; }
    public void setCorrigenda(List<CorrigendumItemDto> corrigenda) { this.corrigenda = corrigenda; }
    public EligibilityEvaluationResultDto getCandidateEligibility() { return candidateEligibility; }
    public void setCandidateEligibility(EligibilityEvaluationResultDto candidateEligibility) { this.candidateEligibility = candidateEligibility; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
