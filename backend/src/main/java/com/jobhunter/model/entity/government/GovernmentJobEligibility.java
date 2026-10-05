package com.jobhunter.model.entity.government;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "government_job_eligibility")
public class GovernmentJobEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private GovernmentJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GenderEligibility gender = GenderEligibility.NOT_SPECIFIED;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "maximum_age")
    private Integer maximumAge;

    @Column(name = "education_json", columnDefinition = "TEXT", nullable = false)
    private String educationJson = "[]";

    @Column(name = "experience_json", columnDefinition = "TEXT", nullable = false)
    private String experienceJson = "[]";

    @Column(name = "experience_years_min", precision = 4, scale = 1)
    private BigDecimal experienceYearsMin = BigDecimal.ZERO;

    @Column(nullable = false, length = 255)
    private String domicile = "NOT_SPECIFIED";

    @Column(name = "category_reservations_json", columnDefinition = "TEXT", nullable = false)
    private String categoryReservationsJson = "[]";

    @Column(name = "pwd_eligible")
    private Boolean pwdEligible = true;

    @Column(name = "ex_serviceman_eligible")
    private Boolean exServicemanEligible = true;

    @Column(name = "other_conditions", columnDefinition = "TEXT")
    private String otherConditions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public GovernmentJobEligibility() {}

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

    public GovernmentJob getJob() { return job; }
    public void setJob(GovernmentJob job) { this.job = job; }

    public GenderEligibility getGender() { return gender; }
    public void setGender(GenderEligibility gender) { this.gender = gender; }

    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }

    public Integer getMaximumAge() { return maximumAge; }
    public void setMaximumAge(Integer maximumAge) { this.maximumAge = maximumAge; }

    public String getEducationJson() { return educationJson; }
    public void setEducationJson(String educationJson) { this.educationJson = educationJson; }

    public String getExperienceJson() { return experienceJson; }
    public void setExperienceJson(String experienceJson) { this.experienceJson = experienceJson; }

    public BigDecimal getExperienceYearsMin() { return experienceYearsMin; }
    public void setExperienceYearsMin(BigDecimal experienceYearsMin) { this.experienceYearsMin = experienceYearsMin; }

    public String getDomicile() { return domicile; }
    public void setDomicile(String domicile) { this.domicile = domicile; }

    public String getCategoryReservationsJson() { return categoryReservationsJson; }
    public void setCategoryReservationsJson(String categoryReservationsJson) { this.categoryReservationsJson = categoryReservationsJson; }

    public Boolean getPwdEligible() { return pwdEligible; }
    public void setPwdEligible(Boolean pwdEligible) { this.pwdEligible = pwdEligible; }

    public Boolean getExServicemanEligible() { return exServicemanEligible; }
    public void setExServicemanEligible(Boolean exServicemanEligible) { this.exServicemanEligible = exServicemanEligible; }

    public String getOtherConditions() { return otherConditions; }
    public void setOtherConditions(String otherConditions) { this.otherConditions = otherConditions; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
