package com.jobhunter.dto.prioritization;

public class KeyTechCoverageDto {
    private String technology;
    private String status; // STRONG (✓), PARTIAL (△), GAP (✕), TRANSFERABLE (~)
    private String evidenceLevel; // COMMERCIAL, PROJECT, NONE

    public KeyTechCoverageDto() {}

    public KeyTechCoverageDto(String technology, String status, String evidenceLevel) {
        this.technology = technology;
        this.status = status;
        this.evidenceLevel = evidenceLevel;
    }

    public String getTechnology() { return technology; }
    public void setTechnology(String technology) { this.technology = technology; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
}
