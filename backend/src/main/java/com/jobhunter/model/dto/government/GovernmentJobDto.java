package com.jobhunter.model.dto.government;

import com.jobhunter.model.entity.government.AuthenticityLevel;
import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GovernmentJobDto {
    private UUID id;
    private String title;
    private String organization;
    private String department;
    private String state;
    private String district;
    private String block;
    private GovernmentEmploymentType employmentType;
    private Integer vacanciesCount;
    private String salary;
    private boolean honorarium;
    private String applicationMode;
    private Instant applicationStartDate;
    private Instant applicationLastDate;
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
    private String genderEligibility;
    private Integer minimumAge;
    private Integer maximumAge;
    private List<String> educationList;
    private String domicile;
    private EligibilityEvaluationResultDto candidateEligibility;
    private int corrigendaCount;
    private Instant createdAt;

    public GovernmentJobDto() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public boolean isHonorarium() { return honorarium; }
    public void setHonorarium(boolean honorarium) { this.honorarium = honorarium; }

    public String getApplicationMode() { return applicationMode; }
    public void setApplicationMode(String applicationMode) { this.applicationMode = applicationMode; }

    public Instant getApplicationStartDate() { return applicationStartDate; }
    public void setApplicationStartDate(Instant applicationStartDate) { this.applicationStartDate = applicationStartDate; }

    public Instant getApplicationLastDate() { return applicationLastDate; }
    public void setApplicationLastDate(Instant applicationLastDate) { this.applicationLastDate = applicationLastDate; }

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

    public String getGenderEligibility() { return genderEligibility; }
    public void setGenderEligibility(String genderEligibility) { this.genderEligibility = genderEligibility; }

    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }

    public Integer getMaximumAge() { return maximumAge; }
    public void setMaximumAge(Integer maximumAge) { this.maximumAge = maximumAge; }

    public List<String> getEducationList() { return educationList; }
    public void setEducationList(List<String> educationList) { this.educationList = educationList; }

    public String getDomicile() { return domicile; }
    public void setDomicile(String domicile) { this.domicile = domicile; }

    public EligibilityEvaluationResultDto getCandidateEligibility() { return candidateEligibility; }
    public void setCandidateEligibility(EligibilityEvaluationResultDto candidateEligibility) { this.candidateEligibility = candidateEligibility; }

    public int getCorrigendaCount() { return corrigendaCount; }
    public void setCorrigendaCount(int corrigendaCount) { this.corrigendaCount = corrigendaCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
