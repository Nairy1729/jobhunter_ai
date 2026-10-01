package com.jobhunter.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ResumeUploadResponse {

    private UUID resumeId;
    private String title;
    private String fileType;
    private Long fileSizeBytes;
    private int characterCount;
    private List<String> detectedSkills;
    private String message;

    // AI Orchestration metadata
    private String headline;
    private BigDecimal yearsOfExperience;
    private int experiencesCount;
    private int projectsCount;
    private String extractionSource;

    public ResumeUploadResponse() {}

    public ResumeUploadResponse(UUID resumeId, String title, String fileType, Long fileSizeBytes, int characterCount, List<String> detectedSkills, String message) {
        this.resumeId = resumeId;
        this.title = title;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.characterCount = characterCount;
        this.detectedSkills = detectedSkills;
        this.message = message;
    }

    public ResumeUploadResponse(UUID resumeId, String title, String fileType, Long fileSizeBytes,
                                int characterCount, List<String> detectedSkills, String message,
                                String headline, BigDecimal yearsOfExperience,
                                int experiencesCount, int projectsCount, String extractionSource) {
        this(resumeId, title, fileType, fileSizeBytes, characterCount, detectedSkills, message);
        this.headline = headline;
        this.yearsOfExperience = yearsOfExperience;
        this.experiencesCount = experiencesCount;
        this.projectsCount = projectsCount;
        this.extractionSource = extractionSource;
    }

    public UUID getResumeId() { return resumeId; }
    public void setResumeId(UUID resumeId) { this.resumeId = resumeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public int getCharacterCount() { return characterCount; }
    public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

    public List<String> getDetectedSkills() { return detectedSkills; }
    public void setDetectedSkills(List<String> detectedSkills) { this.detectedSkills = detectedSkills; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(BigDecimal yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public int getExperiencesCount() { return experiencesCount; }
    public void setExperiencesCount(int experiencesCount) { this.experiencesCount = experiencesCount; }

    public int getProjectsCount() { return projectsCount; }
    public void setProjectsCount(int projectsCount) { this.projectsCount = projectsCount; }

    public String getExtractionSource() { return extractionSource; }
    public void setExtractionSource(String extractionSource) { this.extractionSource = extractionSource; }
}
