package com.jobhunter.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CandidateSkillDto {

    private UUID id;
    private UUID skillId;
    private String skillName;
    private String category;
    private String proficiencyLevel;
    private BigDecimal yearsExperience;
    private boolean primary;
    private String evidenceText;

    public CandidateSkillDto() {}

    public CandidateSkillDto(UUID id, UUID skillId, String skillName, String category, String proficiencyLevel, BigDecimal yearsExperience, boolean primary, String evidenceText) {
        this.id = id;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsExperience = yearsExperience;
        this.primary = primary;
        this.evidenceText = evidenceText;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSkillId() { return skillId; }
    public void setSkillId(UUID skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(String proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }

    public BigDecimal getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(BigDecimal yearsExperience) { this.yearsExperience = yearsExperience; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }
}
