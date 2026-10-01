package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TailoredResumeDetailResponse {

    private UUID id;
    private UUID resumeId;
    private UUID jobDescriptionId;
    private String displayName;
    private String targetCompany;
    private String targetJobTitle;
    private String templateName;
    private String status;

    private TailoredResumePayload.TailoredResumeContent structuredContent;
    private List<SkillClassification> matchedSkills = new ArrayList<>();
    private List<SkillClassification> partiallyMatchedSkills = new ArrayList<>();
    private List<SkillClassification> missingSkills = new ArrayList<>();
    private List<String> tailoringNotes = new ArrayList<>();

    private String latexFilePath;
    private String pdfFilePath;
    private String latexDownloadUrl;
    private String pdfDownloadUrl;
    private BigDecimal atsScoreEstimate;
    private AtsComparisonScoreDto comparison;

    private Instant createdAt;
    private Instant updatedAt;

    public TailoredResumeDetailResponse() {}

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

    public TailoredResumePayload.TailoredResumeContent getStructuredContent() { return structuredContent; }
    public void setStructuredContent(TailoredResumePayload.TailoredResumeContent structuredContent) { this.structuredContent = structuredContent; }

    public List<SkillClassification> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<SkillClassification> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<SkillClassification> getPartiallyMatchedSkills() { return partiallyMatchedSkills; }
    public void setPartiallyMatchedSkills(List<SkillClassification> partiallyMatchedSkills) { this.partiallyMatchedSkills = partiallyMatchedSkills; }

    public List<SkillClassification> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<SkillClassification> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getTailoringNotes() { return tailoringNotes; }
    public void setTailoringNotes(List<String> tailoringNotes) { this.tailoringNotes = tailoringNotes; }

    public String getLatexFilePath() { return latexFilePath; }
    public void setLatexFilePath(String latexFilePath) { this.latexFilePath = latexFilePath; }

    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }

    public String getLatexDownloadUrl() { return latexDownloadUrl; }
    public void setLatexDownloadUrl(String latexDownloadUrl) { this.latexDownloadUrl = latexDownloadUrl; }

    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }

    public BigDecimal getAtsScoreEstimate() { return atsScoreEstimate; }
    public void setAtsScoreEstimate(BigDecimal atsScoreEstimate) { this.atsScoreEstimate = atsScoreEstimate; }

    public AtsComparisonScoreDto getComparison() { return comparison; }
    public void setComparison(AtsComparisonScoreDto comparison) { this.comparison = comparison; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
