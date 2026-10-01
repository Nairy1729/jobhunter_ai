package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TailoredResumeSummaryResponse {

    private UUID id;
    private UUID resumeId;
    private UUID jobDescriptionId;
    private String displayName;
    private String targetCompany;
    private String targetJobTitle;
    private String templateName;
    private String status;
    private BigDecimal atsScoreEstimate;
    private boolean latexAvailable;
    private boolean pdfAvailable;
    private Instant createdAt;
    private Instant updatedAt;

    public TailoredResumeSummaryResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getResumeId() { return resumeId; }
    public void setResumeId(UUID resumeId) { this.resumeId = resumeId; }

    public UUID getJobDescriptionId() { return jobDescriptionId; }
    public void setJobDescriptionId(UUID jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public String getTargetJobTitle() { return targetJobTitle; }
    public void setTargetJobTitle(String targetJobTitle) { this.targetJobTitle = targetJobTitle; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAtsScoreEstimate() { return atsScoreEstimate; }
    public void setAtsScoreEstimate(BigDecimal atsScoreEstimate) { this.atsScoreEstimate = atsScoreEstimate; }

    public boolean isLatexAvailable() { return latexAvailable; }
    public void setLatexAvailable(boolean latexAvailable) { this.latexAvailable = latexAvailable; }

    public boolean isPdfAvailable() { return pdfAvailable; }
    public void setPdfAvailable(boolean pdfAvailable) { this.pdfAvailable = pdfAvailable; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
