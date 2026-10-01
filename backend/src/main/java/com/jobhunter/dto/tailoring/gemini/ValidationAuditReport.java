package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Factual Validation Audit Report from Stage 6.
 * Classifies every claim into SUPPORTED, DERIVED, or UNSUPPORTED.
 */
public class ValidationAuditReport {

    private boolean passed = true;
    private int totalClaimsChecked;
    private int supportedClaims;
    private int derivedClaims;
    private int unsupportedClaims;
    private List<ClaimAuditEntry> entries = new ArrayList<>();
    private List<String> forbiddenInjectedKeywords = new ArrayList<>();

    public static class ClaimAuditEntry {
        public String claimText;
        public String classification; // "SUPPORTED", "DERIVED", "UNSUPPORTED"
        public String rationale;
        public String actionTaken; // "ACCEPTED", "REVERTED_TO_MASTER"

        public ClaimAuditEntry() {}

        public ClaimAuditEntry(String claimText, String classification, String rationale, String actionTaken) {
            this.claimText = claimText;
            this.classification = classification;
            this.rationale = rationale;
            this.actionTaken = actionTaken;
        }
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getTotalClaimsChecked() {
        return totalClaimsChecked;
    }

    public void setTotalClaimsChecked(int totalClaimsChecked) {
        this.totalClaimsChecked = totalClaimsChecked;
    }

    public int getSupportedClaims() {
        return supportedClaims;
    }

    public void setSupportedClaims(int supportedClaims) {
        this.supportedClaims = supportedClaims;
    }

    public int getDerivedClaims() {
        return derivedClaims;
    }

    public void setDerivedClaims(int derivedClaims) {
        this.derivedClaims = derivedClaims;
    }

    public int getUnsupportedClaims() {
        return unsupportedClaims;
    }

    public void setUnsupportedClaims(int unsupportedClaims) {
        this.unsupportedClaims = unsupportedClaims;
    }

    public List<ClaimAuditEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ClaimAuditEntry> entries) {
        this.entries = entries;
    }

    public List<String> getForbiddenInjectedKeywords() {
        return forbiddenInjectedKeywords;
    }

    public void setForbiddenInjectedKeywords(List<String> forbiddenInjectedKeywords) {
        this.forbiddenInjectedKeywords = forbiddenInjectedKeywords;
    }
}
