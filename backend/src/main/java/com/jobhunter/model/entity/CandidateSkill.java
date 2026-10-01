package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_profile_id", "skill_id"}))
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "proficiency_level", nullable = false, length = 50)
    private String proficiencyLevel = "INTERMEDIATE"; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    @Column(name = "years_experience", precision = 3, scale = 1)
    private BigDecimal yearsExperience;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "experience_type", nullable = false, length = 50)
    private String experienceType = "COMMERCIAL"; // COMMERCIAL, PROJECT_ONLY, LEARNING

    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal confidence = new BigDecimal("1.00");

    @Column(name = "evidence_source", length = 255)
    private String evidenceSource;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CandidateSkill() {}

    public CandidateSkill(CandidateProfile candidateProfile, Skill skill, String proficiencyLevel, BigDecimal yearsExperience, boolean primary, String evidenceText) {
        this(candidateProfile, skill, proficiencyLevel, yearsExperience, primary, "COMMERCIAL", new BigDecimal("1.00"), "Profile/Resume", evidenceText);
    }

    public CandidateSkill(CandidateProfile candidateProfile, Skill skill, String proficiencyLevel, BigDecimal yearsExperience,
                          boolean primary, String experienceType, BigDecimal confidence, String evidenceSource, String evidenceText) {
        this.candidateProfile = candidateProfile;
        this.skill = skill;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsExperience = yearsExperience;
        this.primary = primary;
        this.experienceType = experienceType != null ? experienceType : "COMMERCIAL";
        this.confidence = confidence != null ? confidence : new BigDecimal("1.00");
        this.evidenceSource = evidenceSource;
        this.evidenceText = evidenceText;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public String getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(String proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }

    public BigDecimal getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(BigDecimal yearsExperience) { this.yearsExperience = yearsExperience; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public String getExperienceType() { return experienceType; }
    public void setExperienceType(String experienceType) { this.experienceType = experienceType; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getEvidenceSource() { return evidenceSource; }
    public void setEvidenceSource(String evidenceSource) { this.evidenceSource = evidenceSource; }

    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
