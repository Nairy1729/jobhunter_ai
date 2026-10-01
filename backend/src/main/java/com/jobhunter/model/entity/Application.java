package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Simplified Application entity representing the candidate's single application status:
 * "Have I already applied to this job?"
 *
 * Scope boundary:
 * No lifecycle stages, no interview tracking, no rejection tracking, no outcome analytics.
 * Just: candidate + job + applied (boolean) + appliedAt (timestamp).
 */
@Entity
@Table(name = "applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_profile_id", "job_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "applied", nullable = false)
    private boolean applied = true;

    @Column(name = "applied_at")
    private Instant appliedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Application() {}

    public Application(CandidateProfile candidateProfile, Job job, boolean applied) {
        this.candidateProfile = candidateProfile;
        this.job = job;
        this.applied = applied;
        this.appliedAt = applied ? Instant.now() : null;
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

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) {
        this.applied = applied;
        if (applied && this.appliedAt == null) {
            this.appliedAt = Instant.now();
        } else if (!applied) {
            this.appliedAt = null;
        }
    }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
