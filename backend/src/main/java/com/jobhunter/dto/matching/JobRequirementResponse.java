package com.jobhunter.dto.matching;

import java.math.BigDecimal;
import java.util.UUID;

public class JobRequirementResponse {

    private UUID id;
    private String requirementType;
    private String category;
    private String description;
    private String skillName;
    private BigDecimal inferredImportance;
    private boolean isImplied;
    private String rawTextSnippet;

    public JobRequirementResponse() {}

    public JobRequirementResponse(UUID id, String requirementType, String category, String description,
                                  String skillName, BigDecimal inferredImportance, boolean isImplied, String rawTextSnippet) {
        this.id = id;
        this.requirementType = requirementType;
        this.category = category;
        this.description = description;
        this.skillName = skillName;
        this.inferredImportance = inferredImportance;
        this.isImplied = isImplied;
        this.rawTextSnippet = rawTextSnippet;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public BigDecimal getInferredImportance() { return inferredImportance; }
    public void setInferredImportance(BigDecimal inferredImportance) { this.inferredImportance = inferredImportance; }

    public boolean isImplied() { return isImplied; }
    public void setImplied(boolean implied) { isImplied = implied; }

    public String getRawTextSnippet() { return rawTextSnippet; }
    public void setRawTextSnippet(String rawTextSnippet) { this.rawTextSnippet = rawTextSnippet; }
}
