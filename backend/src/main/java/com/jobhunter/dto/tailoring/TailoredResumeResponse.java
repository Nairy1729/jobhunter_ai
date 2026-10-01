package com.jobhunter.dto.tailoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TailoredResumeResponse {

    private UUID id;
    private UUID candidateProfileId;
    private UUID masterResumeId;
    private UUID jobId;
    private int versionNumber;
    private String status;
    private String targetRole;
    private String targetCompany;
    private TailoringPlanDto tailoringPlan;
    private String tailoredMarkdown;
    private String latexSource;
    private boolean pdfAvailable;
    private String pdfDownloadUrl;
    private Long pdfFileSizeBytes;
    private BigDecimal atsScoreEstimate;
    private ResumeValidationReport validationReport;
    private Instant createdAt;
    private Instant updatedAt;

    public TailoredResumeResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateProfileId() { return candidateProfileId; }
    public void setCandidateProfileId(UUID candidateProfileId) { this.candidateProfileId = candidateProfileId; }

    public UUID getMasterResumeId() { return masterResumeId; }
    public void setMasterResumeId(UUID masterResumeId) { this.masterResumeId = masterResumeId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public TailoringPlanDto getTailoringPlan() { return tailoringPlan; }
    public void setTailoringPlan(TailoringPlanDto tailoringPlan) { this.tailoringPlan = tailoringPlan; }

    public String getTailoredMarkdown() { return tailoredMarkdown; }
    public void setTailoredMarkdown(String tailoredMarkdown) { this.tailoredMarkdown = tailoredMarkdown; }

    public String getLatexSource() { return latexSource; }
    public void setLatexSource(String latexSource) { this.latexSource = latexSource; }

    public boolean isPdfAvailable() { return pdfAvailable; }
    public void setPdfAvailable(boolean pdfAvailable) { this.pdfAvailable = pdfAvailable; }

    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }

    public Long getPdfFileSizeBytes() { return pdfFileSizeBytes; }
    public void setPdfFileSizeBytes(Long pdfFileSizeBytes) { this.pdfFileSizeBytes = pdfFileSizeBytes; }

    public BigDecimal getAtsScoreEstimate() { return atsScoreEstimate; }
    public void setAtsScoreEstimate(BigDecimal atsScoreEstimate) { this.atsScoreEstimate = atsScoreEstimate; }

    public ResumeValidationReport getValidationReport() { return validationReport; }
    public void setValidationReport(ResumeValidationReport validationReport) { this.validationReport = validationReport; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
