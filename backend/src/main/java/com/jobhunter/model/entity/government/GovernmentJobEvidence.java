package com.jobhunter.model.entity.government;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "government_job_evidence")
public class GovernmentJobEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private GovernmentJob job;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @Column(name = "source_document", length = 500)
    private String sourceDocument;

    @Column(name = "page_or_section", length = 100)
    private String pageOrSection;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public GovernmentJobEvidence() {}

    public GovernmentJobEvidence(GovernmentJob job, String fieldName, String fieldValue, String sourceDocument, String pageOrSection, String excerpt) {
        this.job = job;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.sourceDocument = sourceDocument;
        this.pageOrSection = pageOrSection;
        this.excerpt = excerpt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public GovernmentJob getJob() { return job; }
    public void setJob(GovernmentJob job) { this.job = job; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public String getSourceDocument() { return sourceDocument; }
    public void setSourceDocument(String sourceDocument) { this.sourceDocument = sourceDocument; }

    public String getPageOrSection() { return pageOrSection; }
    public void setPageOrSection(String pageOrSection) { this.pageOrSection = pageOrSection; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public Instant getCreatedAt() { return createdAt; }
}
