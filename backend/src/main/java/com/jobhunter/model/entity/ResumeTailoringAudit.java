package com.jobhunter.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_tailoring_audits")
public class ResumeTailoringAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_resume_id")
    private TailoredResume tailoredResume;

    @Column(name = "source_resume_version", nullable = false)
    private int sourceResumeVersion = 1;

    @Column(name = "validator_version", nullable = false, length = 50)
    private String validatorVersion = "v2.0-claim-auditor";

    @Column(name = "claims_checked", nullable = false)
    private int claimsChecked = 0;

    @Column(name = "claims_passed", nullable = false)
    private int claimsPassed = 0;

    @Column(name = "claims_failed", nullable = false)
    private int claimsFailed = 0;

    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus; // READY_FOR_DOWNLOAD, VALIDATION_FAILED, BLOCKED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_details", columnDefinition = "jsonb", nullable = false)
    private String auditDetails = "{}";

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    public ResumeTailoringAudit() {}

    public ResumeTailoringAudit(
            CandidateProfile candidateProfile,
            Job job,
            TailoredResume tailoredResume,
            int sourceResumeVersion,
            String validatorVersion,
            int claimsChecked,
            int claimsPassed,
            int claimsFailed,
            String validationStatus,
            String failureReason,
            String auditDetails) {
        this.candidateProfile = candidateProfile;
        this.job = job;
        this.tailoredResume = tailoredResume;
        this.sourceResumeVersion = sourceResumeVersion;
        this.validatorVersion = validatorVersion;
        this.claimsChecked = claimsChecked;
        this.claimsPassed = claimsPassed;
        this.claimsFailed = claimsFailed;
        this.validationStatus = validationStatus;
        this.failureReason = failureReason;
        this.auditDetails = auditDetails != null ? auditDetails : "{}";
        this.generatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public TailoredResume getTailoredResume() { return tailoredResume; }
    public void setTailoredResume(TailoredResume tailoredResume) { this.tailoredResume = tailoredResume; }

    public int getSourceResumeVersion() { return sourceResumeVersion; }
    public void setSourceResumeVersion(int sourceResumeVersion) { this.sourceResumeVersion = sourceResumeVersion; }

    public String getValidatorVersion() { return validatorVersion; }
    public void setValidatorVersion(String validatorVersion) { this.validatorVersion = validatorVersion; }

    public int getClaimsChecked() { return claimsChecked; }
    public void setClaimsChecked(int claimsChecked) { this.claimsChecked = claimsChecked; }

    public int getClaimsPassed() { return claimsPassed; }
    public void setClaimsPassed(int claimsPassed) { this.claimsPassed = claimsPassed; }

    public int getClaimsFailed() { return claimsFailed; }
    public void setClaimsFailed(int claimsFailed) { this.claimsFailed = claimsFailed; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getAuditDetails() { return auditDetails; }
    public void setAuditDetails(String auditDetails) { this.auditDetails = auditDetails; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
