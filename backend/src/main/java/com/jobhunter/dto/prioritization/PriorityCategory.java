package com.jobhunter.dto.prioritization;

public enum PriorityCategory {
    HIGH_PRIORITY("High Priority", "Strong must-have alignment, proven evidence, no hard constraint conflicts"),
    MEDIUM_PRIORITY("Medium Priority", "Good alignment but one or more meaningful gaps or uncertainties"),
    LOW_PRIORITY("Low Priority", "Some relevance but substantial gaps or weak alignment"),
    NOT_RECOMMENDED("Not Recommended", "Major hard constraint incompatibility or insufficient relevance");

    private final String displayName;
    private final String description;

    PriorityCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
