package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_requirements")
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "requirement_type", nullable = false, length = 50)
    private String requirementType; // MUST_HAVE, NICE_TO_HAVE, LATENT_SIGNAL, RESPONSIBILITIES, SENIORITY, DOMAIN, EDUCATION, LOCATION, WORK_MODE, EXPERIENCE, TECHNOLOGY, SOFT_SKILLS

    @Column(nullable = false, length = 50)
    private String category; // TECHNICAL, EXPERIENCE, EDUCATION, PROCESS, DOMAIN, SOFT_SKILL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "inferred_importance", nullable = false, precision = 3, scale = 2)
    private BigDecimal inferredImportance = new BigDecimal("1.00");

    @Column(name = "is_implied", nullable = false)
    private boolean implied = false;

    @Column(name = "raw_text_snippet", columnDefinition = "TEXT")
    private String rawTextSnippet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public JobRequirement() {}

    public JobRequirement(Job job, String requirementType, String category, String description, BigDecimal inferredImportance, boolean implied, String rawTextSnippet) {
        this.job = job;
        this.requirementType = requirementType;
        this.category = category;
        this.description = description;
        this.inferredImportance = inferredImportance != null ? inferredImportance : BigDecimal.ONE;
        this.implied = implied;
        this.rawTextSnippet = rawTextSnippet;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public BigDecimal getInferredImportance() { return inferredImportance; }
    public void setInferredImportance(BigDecimal inferredImportance) { this.inferredImportance = inferredImportance; }

    public boolean isImplied() { return implied; }
    public void setImplied(boolean implied) { this.implied = implied; }

    public String getRawTextSnippet() { return rawTextSnippet; }
    public void setRawTextSnippet(String rawTextSnippet) { this.rawTextSnippet = rawTextSnippet; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
