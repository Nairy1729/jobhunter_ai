package com.jobhunter.dto.matching;

import java.util.ArrayList;
import java.util.List;

public class UsefulnessDecision {

    private boolean useful;
    private String usefulnessStatus; // "USEFUL" or "NOT_USEFUL"
    private String matchCategory;    // "HIGH_RELEVANCE", "GOOD_MATCH", "POSSIBLE_MATCH", "NOT_USEFUL"
    private String rationale;
    private List<String> matchedRequirements = new ArrayList<>();
    private List<String> missingRequirements = new ArrayList<>();
    private List<String> candidateEvidence = new ArrayList<>();

    public UsefulnessDecision() {}

    public UsefulnessDecision(boolean useful, String usefulnessStatus, String matchCategory, String rationale) {
        this.useful = useful;
        this.usefulnessStatus = usefulnessStatus;
        this.matchCategory = matchCategory;
        this.rationale = rationale;
    }

    public boolean isUseful() { return useful; }
    public void setUseful(boolean useful) { this.useful = useful; }

    public String getUsefulnessStatus() { return usefulnessStatus; }
    public void setUsefulnessStatus(String usefulnessStatus) { this.usefulnessStatus = usefulnessStatus; }

    public String getMatchCategory() { return matchCategory; }
    public void setMatchCategory(String matchCategory) { this.matchCategory = matchCategory; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public List<String> getMatchedRequirements() { return matchedRequirements; }
    public void setMatchedRequirements(List<String> matchedRequirements) { this.matchedRequirements = matchedRequirements; }

    public List<String> getMissingRequirements() { return missingRequirements; }
    public void setMissingRequirements(List<String> missingRequirements) { this.missingRequirements = missingRequirements; }

    public List<String> getCandidateEvidence() { return candidateEvidence; }
    public void setCandidateEvidence(List<String> candidateEvidence) { this.candidateEvidence = candidateEvidence; }
}
