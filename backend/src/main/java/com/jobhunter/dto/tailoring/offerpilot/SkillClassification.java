package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkillClassification {

    private String skill;
    private String matchType; // MATCHED, PARTIALLY_MATCHED, MISSING
    private String evidence;

    public SkillClassification() {}

    public SkillClassification(String skill, String matchType, String evidence) {
        this.skill = skill;
        this.matchType = matchType;
        this.evidence = evidence;
    }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}
