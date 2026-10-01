package com.jobhunter.dto.prioritization;

public enum JobFreshness {
    NEW("New", "Posted within the last 7 days"),
    RECENT("Recent", "Posted between 8 and 30 days ago"),
    OLDER("Older", "Posted between 31 and 60 days ago"),
    STALE("Stale", "Posted more than 60 days ago; lower application priority"),
    UNKNOWN("Unknown", "No reliable posting date disclosed by employer source");

    private final String label;
    private final String description;

    JobFreshness(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
