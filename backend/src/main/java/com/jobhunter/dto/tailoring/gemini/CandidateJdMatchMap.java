package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Match mapping comparing Master Resume against Job Description.
 * Stage 3 of the Gemini AI Resume Tailoring Pipeline.
 */
public class CandidateJdMatchMap {

    public List<RequirementMatchItem> mappings = new ArrayList<>();
    public List<String> matchedSkills = new ArrayList<>();
    public List<String> partiallyMatchedSkills = new ArrayList<>();
    public List<String> missingSkills = new ArrayList<>();

    public static class RequirementMatchItem {
        public String requirement;
        public String candidateEvidence;
        public String status; // "MATCHED", "PARTIALLY_MATCHED", "MISSING"

        public RequirementMatchItem() {}

        public RequirementMatchItem(String requirement, String candidateEvidence, String status) {
            this.requirement = requirement;
            this.candidateEvidence = candidateEvidence;
            this.status = status;
        }
    }
}
