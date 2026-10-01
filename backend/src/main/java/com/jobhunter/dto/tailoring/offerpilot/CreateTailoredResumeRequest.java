package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTailoredResumeRequest {

    private UUID resumeId;
    private UUID jobDescriptionId;
    private String displayName;
    private String templateName = "PROFESSIONAL_DEFAULT";

    public CreateTailoredResumeRequest() {}

    public CreateTailoredResumeRequest(UUID resumeId, UUID jobDescriptionId, String displayName, String templateName) {
        this.resumeId = resumeId;
        this.jobDescriptionId = jobDescriptionId;
        this.displayName = displayName;
        if (templateName != null && !templateName.isBlank()) {
            this.templateName = templateName;
        }
    }

    public UUID getResumeId() { return resumeId; }
    public void setResumeId(UUID resumeId) { this.resumeId = resumeId; }

    public UUID getJobDescriptionId() { return jobDescriptionId; }
    public void setJobDescriptionId(UUID jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
}
