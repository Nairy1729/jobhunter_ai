package com.jobhunter.dto.profile;

public class ReadinessItem {

    private String key;
    private String label;
    private boolean satisfied;
    private boolean mandatory;
    private String details;

    public ReadinessItem() {}

    public ReadinessItem(String key, String label, boolean satisfied, boolean mandatory, String details) {
        this.key = key;
        this.label = label;
        this.satisfied = satisfied;
        this.mandatory = mandatory;
        this.details = details;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isSatisfied() { return satisfied; }
    public void setSatisfied(boolean satisfied) { this.satisfied = satisfied; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
