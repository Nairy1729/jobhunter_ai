package com.jobhunter.dto.prioritization;

public class RequirementCoverageItem {
    private String requirement;
    private String importance; // MUST_HAVE, PREFERRED, NICE_TO_HAVE
    private String candidateEvidence; // COMMERCIAL, PROJECT, LEARNING, NONE, TRANSFERABLE
    private String coverage; // STRONG, PARTIAL, TRANSFERABLE, GAP
    private String explanation;

    public RequirementCoverageItem() {}

    public RequirementCoverageItem(String requirement, String importance, String candidateEvidence,
                                  String coverage, String explanation) {
        this.requirement = requirement;
        this.importance = importance;
        this.candidateEvidence = candidateEvidence;
        this.coverage = coverage;
        this.explanation = explanation;
    }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public String getCandidateEvidence() { return candidateEvidence; }
    public void setCandidateEvidence(String candidateEvidence) { this.candidateEvidence = candidateEvidence; }

    public String getCoverage() { return coverage; }
    public void setCoverage(String coverage) { this.coverage = coverage; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
