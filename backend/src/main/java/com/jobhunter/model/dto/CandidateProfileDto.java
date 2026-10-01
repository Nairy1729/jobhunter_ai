package com.jobhunter.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CandidateProfileDto {

    private UUID id;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String headline;
    private String summary;
    private BigDecimal yearsOfExperience;
    private String currentLocation;
    private List<String> preferredLocations = new ArrayList<>();
    private List<String> workModes = new ArrayList<>();
    private BigDecimal minSalaryInr;
    private String currency;
    private List<String> targetRoles = new ArrayList<>();
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private String phoneNumber;
    private List<CandidateSkillDto> skills = new ArrayList<>();
    private String masterResumeTitle;
    private UUID masterResumeId;

    public CandidateProfileDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(BigDecimal yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public List<String> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<String> preferredLocations) { this.preferredLocations = preferredLocations; }

    public List<String> getWorkModes() { return workModes; }
    public void setWorkModes(List<String> workModes) { this.workModes = workModes; }

    public BigDecimal getMinSalaryInr() { return minSalaryInr; }
    public void setMinSalaryInr(BigDecimal minSalaryInr) { this.minSalaryInr = minSalaryInr; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<String> getTargetRoles() { return targetRoles; }
    public void setTargetRoles(List<String> targetRoles) { this.targetRoles = targetRoles; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public List<CandidateSkillDto> getSkills() { return skills; }
    public void setSkills(List<CandidateSkillDto> skills) { this.skills = skills; }

    public String getMasterResumeTitle() { return masterResumeTitle; }
    public void setMasterResumeTitle(String masterResumeTitle) { this.masterResumeTitle = masterResumeTitle; }

    public UUID getMasterResumeId() { return masterResumeId; }
    public void setMasterResumeId(UUID masterResumeId) { this.masterResumeId = masterResumeId; }
}
