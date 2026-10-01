package com.jobhunter.dto.matching;

public class RequirementMatchItem {
    private String requirement;
    private String category;
    private String candidateEvidence;
    private String matchType; // STRONG, PARTIAL, TRANSFERABLE, GAP
    private String confidence; // HIGH, MEDIUM, LOW
    private String explanation;
    private boolean isImplied;

    public RequirementMatchItem() {}

    public RequirementMatchItem(String requirement, String category, String candidateEvidence,
                                String matchType, String confidence, String explanation, boolean isImplied) {
        this.requirement = requirement;
        this.category = category;
        this.candidateEvidence = candidateEvidence;
        this.matchType = matchType;
        this.confidence = confidence;
        this.explanation = explanation;
        this.isImplied = isImplied;
    }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCandidateEvidence() { return candidateEvidence; }
    public void setCandidateEvidence(String candidateEvidence) { this.candidateEvidence = candidateEvidence; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public boolean isImplied() { return isImplied; }
    public void setImplied(boolean implied) { isImplied = implied; }
}
