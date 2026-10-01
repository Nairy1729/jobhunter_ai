package com.jobhunter.dto.tailoring;

import com.jobhunter.model.fact.ClaimType;
import java.util.UUID;

public class AuditedClaim {

    private ClaimType claimType;
    private String text;
    private UUID sourceFactId;
    private String status; // SUPPORTED, UNSUPPORTED, AMBIGUOUS
    private String classification; // DIRECTLY_SUPPORTED, DERIVED_FROM_SUPPORTED_FACTS, UNSUPPORTED
    private String failureReason;
    private String expectedEvidence;

    public AuditedClaim() {}

    public AuditedClaim(ClaimType claimType, String text, UUID sourceFactId, String status, String failureReason, String expectedEvidence) {
        this.claimType = claimType;
        this.text = text;
        this.sourceFactId = sourceFactId;
        this.status = status;
        this.classification = "SUPPORTED".equalsIgnoreCase(status) ? "DIRECTLY_SUPPORTED" : "UNSUPPORTED";
        this.failureReason = failureReason;
        this.expectedEvidence = expectedEvidence;
    }

    public AuditedClaim(ClaimType claimType, String text, UUID sourceFactId, String status, String classification, String failureReason, String expectedEvidence) {
        this.claimType = claimType;
        this.text = text;
        this.sourceFactId = sourceFactId;
        this.status = status;
        this.classification = classification;
        this.failureReason = failureReason;
        this.expectedEvidence = expectedEvidence;
    }

    public static AuditedClaim supported(ClaimType claimType, String text, UUID sourceFactId) {
        return directlySupported(claimType, text, sourceFactId);
    }

    public static AuditedClaim directlySupported(ClaimType claimType, String text, UUID sourceFactId) {
        return new AuditedClaim(claimType, text, sourceFactId, "SUPPORTED", "DIRECTLY_SUPPORTED", null, null);
    }

    public static AuditedClaim derivedSupported(ClaimType claimType, String text, UUID sourceFactId, String derivationNote) {
        return new AuditedClaim(claimType, text, sourceFactId, "SUPPORTED", "DERIVED_FROM_SUPPORTED_FACTS", null, derivationNote);
    }

    public static AuditedClaim unsupported(ClaimType claimType, String text, String failureReason, String expectedEvidence) {
        return new AuditedClaim(claimType, text, null, "UNSUPPORTED", "UNSUPPORTED", failureReason, expectedEvidence);
    }

    public static AuditedClaim ambiguous(ClaimType claimType, String text, String failureReason, String expectedEvidence) {
        return new AuditedClaim(claimType, text, null, "AMBIGUOUS", "UNSUPPORTED", failureReason, expectedEvidence);
    }

    public ClaimType getClaimType() { return claimType; }
    public void setClaimType(ClaimType claimType) { this.claimType = claimType; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public UUID getSourceFactId() { return sourceFactId; }
    public void setSourceFactId(UUID sourceFactId) { this.sourceFactId = sourceFactId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getExpectedEvidence() { return expectedEvidence; }
    public void setExpectedEvidence(String expectedEvidence) { this.expectedEvidence = expectedEvidence; }
}
