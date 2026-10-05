package com.jobhunter.model.entity.government;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobhunter.model.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_government_profiles")
public class CandidateGovernmentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer age;

    private LocalDate dob;

    @Column(length = 40)
    private String gender;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    @Column(name = "domicile_state", length = 100)
    private String domicileState;

    @Column(name = "domicile_district", length = 100)
    private String domicileDistrict;

    @Column(name = "highest_education", length = 100)
    private String highestEducation;

    @Column(name = "degrees_json", columnDefinition = "TEXT", nullable = false)
    private String degreesJson = "[]";

    @Column(name = "passing_year")
    private Integer passingYear;

    @Column(name = "years_of_experience", nullable = false, precision = 4, scale = 1)
    private BigDecimal yearsOfExperience = BigDecimal.ZERO;

    @Column(nullable = false, length = 50)
    private String category = "UR/GEN";

    @Column(nullable = false)
    private boolean pwd = false;

    @Column(name = "ex_serviceman", nullable = false)
    private boolean exServiceman = false;

    @Column(name = "preferred_states_json", columnDefinition = "TEXT", nullable = false)
    private String preferredStatesJson = "[]";

    @Column(name = "preferred_districts_json", columnDefinition = "TEXT", nullable = false)
    private String preferredDistrictsJson = "[]";

    @Column(name = "preferred_employment_types_json", columnDefinition = "TEXT", nullable = false)
    private String preferredEmploymentTypesJson = "[]";

    @Column(name = "skills_json", columnDefinition = "TEXT", nullable = false)
    private String skillsJson = "[]";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CandidateGovernmentProfile() {}

    public CandidateGovernmentProfile(User user) {
        this.user = user;
    }

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDomicileState() { return domicileState; }
    public void setDomicileState(String domicileState) { this.domicileState = domicileState; }

    public String getDomicileDistrict() { return domicileDistrict; }
    public void setDomicileDistrict(String domicileDistrict) { this.domicileDistrict = domicileDistrict; }

    public String getHighestEducation() { return highestEducation; }
    public void setHighestEducation(String highestEducation) { this.highestEducation = highestEducation; }

    public String getDegreesJson() { return degreesJson; }
    public void setDegreesJson(String degreesJson) { this.degreesJson = degreesJson; }

    public Integer getPassingYear() { return passingYear; }
    public void setPassingYear(Integer passingYear) { this.passingYear = passingYear; }

    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(BigDecimal yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isPwd() { return pwd; }
    public void setPwd(boolean pwd) { this.pwd = pwd; }

    public boolean isExServiceman() { return exServiceman; }
    public void setExServiceman(boolean exServiceman) { this.exServiceman = exServiceman; }

    public String getPreferredStatesJson() { return preferredStatesJson; }
    public void setPreferredStatesJson(String preferredStatesJson) { this.preferredStatesJson = preferredStatesJson; }

    public String getPreferredDistrictsJson() { return preferredDistrictsJson; }
    public void setPreferredDistrictsJson(String preferredDistrictsJson) { this.preferredDistrictsJson = preferredDistrictsJson; }

    public String getPreferredEmploymentTypesJson() { return preferredEmploymentTypesJson; }
    public void setPreferredEmploymentTypesJson(String preferredEmploymentTypesJson) { this.preferredEmploymentTypesJson = preferredEmploymentTypesJson; }

    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
