package com.jobhunter.model.dto;

import java.time.Instant;
import java.util.UUID;

public class JobAppliedStatusDto {

    private UUID jobId;
    private boolean applied;
    private Instant appliedAt;

    public JobAppliedStatusDto() {}

    public JobAppliedStatusDto(UUID jobId, boolean applied, Instant appliedAt) {
        this.jobId = jobId;
        this.applied = applied;
        this.appliedAt = appliedAt;
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
