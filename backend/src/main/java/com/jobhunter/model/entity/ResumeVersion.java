package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_versions")
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_resume_id", nullable = false)
    private Resume masterResume;

    @Column(name = "version_label", nullable = false, length = 150)
    private String versionLabel;

    @Column(name = "tailored_markdown", columnDefinition = "TEXT", nullable = false)
    private String tailoredMarkdown;

    @Column(name = "rendered_file_path", length = 500)
    private String renderedFilePath;

    @Column(name = "diff_payload", columnDefinition = "jsonb", nullable = false)
    private String diffPayload = "{}";

    @Column(name = "generation_prompt_hash", length = 64)
    private String generationPromptHash;

    @Column(name = "ats_score_estimate", precision = 5, scale = 2)
    private BigDecimal atsScoreEstimate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ResumeVersion() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Resume getMasterResume() { return masterResume; }
    public void setMasterResume(Resume masterResume) { this.masterResume = masterResume; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getTailoredMarkdown() { return tailoredMarkdown; }
    public void setTailoredMarkdown(String tailoredMarkdown) { this.tailoredMarkdown = tailoredMarkdown; }

    public String getRenderedFilePath() { return renderedFilePath; }
    public void setRenderedFilePath(String renderedFilePath) { this.renderedFilePath = renderedFilePath; }

    public String getDiffPayload() { return diffPayload; }
    public void setDiffPayload(String diffPayload) { this.diffPayload = diffPayload; }

    public String getGenerationPromptHash() { return generationPromptHash; }
    public void setGenerationPromptHash(String generationPromptHash) { this.generationPromptHash = generationPromptHash; }

    public BigDecimal getAtsScoreEstimate() { return atsScoreEstimate; }
    public void setAtsScoreEstimate(BigDecimal atsScoreEstimate) { this.atsScoreEstimate = atsScoreEstimate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
