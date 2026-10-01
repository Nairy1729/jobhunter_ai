package com.jobhunter.dto.tailoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TailoringAuditReportDto {

    private UUID tailoredResumeId;
    private UUID candidateProfileId;
    private UUID jobId;
    private String validatorVersion = "v2.0-claim-auditor";
    private boolean passed = false;
    private String overallStatus = "VALIDATION_FAILED"; // READY_FOR_DOWNLOAD, VALIDATION_FAILED
    private String statusMessage;
    private int claimsChecked = 0;
    private int claimsPassed = 0;
    private int claimsFailed = 0;
    private List<AuditedClaim> unsupportedClaims = new ArrayList<>();
    private List<AuditedClaim> passedClaims = new ArrayList<>();
    private List<String> allowedOrganizations = new ArrayList<>();
    private List<String> allowedDegrees = new ArrayList<>();
    private Instant auditedAt = Instant.now();

    public TailoringAuditReportDto() {}

    public UUID getTailoredResumeId() { return tailoredResumeId; }
    public void setTailoredResumeId(UUID tailoredResumeId) { this.tailoredResumeId = tailoredResumeId; }

    public UUID getCandidateProfileId() { return candidateProfileId; }
    public void setCandidateProfileId(UUID candidateProfileId) { this.candidateProfileId = candidateProfileId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getValidatorVersion() { return validatorVersion; }
    public void setValidatorVersion(String validatorVersion) { this.validatorVersion = validatorVersion; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public int getClaimsChecked() { return claimsChecked; }
    public void setClaimsChecked(int claimsChecked) { this.claimsChecked = claimsChecked; }

    public int getClaimsPassed() { return claimsPassed; }
    public void setClaimsPassed(int claimsPassed) { this.claimsPassed = claimsPassed; }

    public int getClaimsFailed() { return claimsFailed; }
    public void setClaimsFailed(int claimsFailed) { this.claimsFailed = claimsFailed; }

    public List<AuditedClaim> getUnsupportedClaims() { return unsupportedClaims; }
    public void setUnsupportedClaims(List<AuditedClaim> unsupportedClaims) { this.unsupportedClaims = unsupportedClaims; }

    public List<AuditedClaim> getPassedClaims() { return passedClaims; }
    public void setPassedClaims(List<AuditedClaim> passedClaims) { this.passedClaims = passedClaims; }

    public List<String> getAllowedOrganizations() { return allowedOrganizations; }
    public void setAllowedOrganizations(List<String> allowedOrganizations) { this.allowedOrganizations = allowedOrganizations; }

    public List<String> getAllowedDegrees() { return allowedDegrees; }
    public void setAllowedDegrees(List<String> allowedDegrees) { this.allowedDegrees = allowedDegrees; }

    public Instant getAuditedAt() { return auditedAt; }
    public void setAuditedAt(Instant auditedAt) { this.auditedAt = auditedAt; }
}
