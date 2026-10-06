package com.jobhunter.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tailored_resumes")
public class TailoredResume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_resume_id")
    private Resume masterResume;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "GENERATED"; // DRAFT, GENERATED, USER_REVIEW, APPROVED, EXPORTED

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "target_company", nullable = false)
    private String targetCompany;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tailoring_plan", columnDefinition = "jsonb", nullable = false)
    private String tailoringPlan = "{}";

    @Column(name = "tailored_markdown", columnDefinition = "TEXT", nullable = false)
    private String tailoredMarkdown;

    @Column(name = "latex_source", columnDefinition = "TEXT")
    private String latexSource;

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    @Column(name = "pdf_file_size_bytes")
    private Long pdfFileSizeBytes;

    @Column(name = "ats_score_estimate", precision = 5, scale = 2)
    private BigDecimal atsScoreEstimate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_report", columnDefinition = "jsonb", nullable = false)
    private String validationReport = "{}";

    @Column(name = "generation_prompt_hash", length = 64)
    private String generationPromptHash;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "target_job_title", length = 150)
    private String targetJobTitle;

    @Column(name = "template_name", nullable = false, length = 80)
    private String templateName = "PROFESSIONAL_DEFAULT";

    @Column(name = "structured_content_json", columnDefinition = "TEXT")
    private String structuredContentJson;

    @Column(name = "matched_skills_json", columnDefinition = "TEXT")
    private String matchedSkillsJson;

    @Column(name = "partially_matched_skills_json", columnDefinition = "TEXT")
    private String partiallyMatchedSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "TEXT")
    private String missingSkillsJson;

    @Column(name = "tailoring_notes_json", columnDefinition = "TEXT")
    private String tailoringNotesJson;

    @Column(name = "latex_file_path", length = 1000)
    private String latexFilePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public TailoredResume() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public Resume getMasterResume() { return masterResume; }
    public void setMasterResume(Resume masterResume) { this.masterResume = masterResume; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public String getTailoringPlan() { return tailoringPlan; }
    public void setTailoringPlan(String tailoringPlan) { this.tailoringPlan = tailoringPlan; }

    public String getTailoredMarkdown() { return tailoredMarkdown; }
    public void setTailoredMarkdown(String tailoredMarkdown) { this.tailoredMarkdown = tailoredMarkdown; }

    public String getLatexSource() { return latexSource; }
    public void setLatexSource(String latexSource) { this.latexSource = latexSource; }

    public String getPdfFilePath() { return pdfFilePath; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }

    public Long getPdfFileSizeBytes() { return pdfFileSizeBytes; }
    public void setPdfFileSizeBytes(Long pdfFileSizeBytes) { this.pdfFileSizeBytes = pdfFileSizeBytes; }

    public BigDecimal getAtsScoreEstimate() { return atsScoreEstimate; }
    public void setAtsScoreEstimate(BigDecimal atsScoreEstimate) { this.atsScoreEstimate = atsScoreEstimate; }

    public String getValidationReport() { return validationReport; }
    public void setValidationReport(String validationReport) { this.validationReport = validationReport; }

    public String getGenerationPromptHash() { return generationPromptHash; }
    public void setGenerationPromptHash(String generationPromptHash) { this.generationPromptHash = generationPromptHash; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTargetJobTitle() { return targetJobTitle; }
    public void setTargetJobTitle(String targetJobTitle) { this.targetJobTitle = targetJobTitle; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getStructuredContentJson() { return structuredContentJson; }
    public void setStructuredContentJson(String structuredContentJson) { this.structuredContentJson = structuredContentJson; }

    public String getMatchedSkillsJson() { return matchedSkillsJson; }
    public void setMatchedSkillsJson(String matchedSkillsJson) { this.matchedSkillsJson = matchedSkillsJson; }

    public String getPartiallyMatchedSkillsJson() { return partiallyMatchedSkillsJson; }
    public void setPartiallyMatchedSkillsJson(String partiallyMatchedSkillsJson) { this.partiallyMatchedSkillsJson = partiallyMatchedSkillsJson; }

    public String getMissingSkillsJson() { return missingSkillsJson; }
    public void setMissingSkillsJson(String missingSkillsJson) { this.missingSkillsJson = missingSkillsJson; }

    public String getTailoringNotesJson() { return tailoringNotesJson; }
    public void setTailoringNotesJson(String tailoringNotesJson) { this.tailoringNotesJson = tailoringNotesJson; }

    public String getLatexFilePath() { return latexFilePath; }
    public void setLatexFilePath(String latexFilePath) { this.latexFilePath = latexFilePath; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
