package com.jobhunter.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "years_of_experience", nullable = false, precision = 4, scale = 1)
    private BigDecimal yearsOfExperience = BigDecimal.ZERO;

    @Column(name = "current_location", length = 150)
    private String currentLocation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_locations", columnDefinition = "jsonb", nullable = false)
    private String preferredLocations = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "work_modes", columnDefinition = "jsonb", nullable = false)
    private String workModes = "[]";

    @Column(name = "min_salary_inr", precision = 12, scale = 2)
    private BigDecimal minSalaryInr = new BigDecimal("1000000.00");

    @Column(length = 10, nullable = false)
    private String currency = "INR";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_roles", columnDefinition = "jsonb", nullable = false)
    private String targetRoles = "[]";

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_profile_data", columnDefinition = "jsonb", nullable = false)
    private String rawProfileData = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes = new ArrayList<>();

    public CandidateProfile() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(BigDecimal yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(String preferredLocations) { this.preferredLocations = preferredLocations; }

    public String getWorkModes() { return workModes; }
    public void setWorkModes(String workModes) { this.workModes = workModes; }

    public BigDecimal getMinSalaryInr() { return minSalaryInr; }
    public void setMinSalaryInr(BigDecimal minSalaryInr) { this.minSalaryInr = minSalaryInr; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTargetRoles() { return targetRoles; }
    public void setTargetRoles(String targetRoles) { this.targetRoles = targetRoles; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRawProfileData() { return rawProfileData; }
    public void setRawProfileData(String rawProfileData) { this.rawProfileData = rawProfileData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<CandidateSkill> getSkills() { return skills; }
    public void setSkills(List<CandidateSkill> skills) { this.skills = skills; }

    public List<CandidateExperience> getExperiences() { return experiences; }
    public void setExperiences(List<CandidateExperience> experiences) { this.experiences = experiences; }

    public List<CandidateProject> getProjects() { return projects; }
    public void setProjects(List<CandidateProject> projects) { this.projects = projects; }

    public List<Resume> getResumes() { return resumes; }
    public void setResumes(List<Resume> resumes) { this.resumes = resumes; }
}
