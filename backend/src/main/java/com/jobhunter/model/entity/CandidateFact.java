package com.jobhunter.model.entity;

import com.jobhunter.model.fact.EvidenceLevel;
import com.jobhunter.model.fact.FactCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Canonical Candidate Fact Store.
 * The absolute, immutable source of truth for candidate qualifications.
 * Only facts with evidenceLevel VERIFIED or SUPPORTED may be cited in a tailored resume.
 * UNKNOWN facts are strictly prohibited from being used.
 */
@Entity
@Table(name = "candidate_facts")
public class CandidateFact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FactCategory category;

    @Column(name = "fact_value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @Column(nullable = false, length = 100)
    private String source; // PROFILE, MASTER_RESUME, EXPERIENCE, PROJECT, SKILL, EDUCATION, MANUAL

    @Column(name = "source_reference")
    private String sourceReference; // e.g., experience ID, resume bullet index, certificate ID

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_level", nullable = false, length = 50)
    private EvidenceLevel evidenceLevel = EvidenceLevel.VERIFIED;

    @Column(nullable = false)
    private boolean verified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CandidateFact() {}

    public CandidateFact(
            CandidateProfile candidateProfile,
            FactCategory category,
            String value,
            String source,
            String sourceReference,
            EvidenceLevel evidenceLevel,
            boolean verified) {
        this.candidateProfile = candidateProfile;
        this.category = category;
        this.value = value;
        this.source = source;
        this.sourceReference = sourceReference;
        this.evidenceLevel = evidenceLevel;
        this.verified = verified;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public FactCategory getCategory() { return category; }
    public void setCategory(FactCategory category) { this.category = category; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public EvidenceLevel getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(EvidenceLevel evidenceLevel) { this.evidenceLevel = evidenceLevel; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAllowedOnResume() {
        return this.verified && (this.evidenceLevel == EvidenceLevel.VERIFIED || this.evidenceLevel == EvidenceLevel.SUPPORTED);
    }
}
