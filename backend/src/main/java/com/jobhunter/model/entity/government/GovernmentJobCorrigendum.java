package com.jobhunter.model.entity.government;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "government_job_corrigenda")
public class GovernmentJobCorrigendum {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private GovernmentJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 60)
    private CorrigendumType noticeType = CorrigendumType.CORRIGENDUM;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "document_url", length = 1000)
    private String documentUrl;

    @Column(name = "issue_date")
    private Instant issueDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "revised_last_date")
    private Instant revisedLastDate;

    @Column(name = "revised_vacancies")
    private Integer revisedVacancies;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public GovernmentJobCorrigendum() {}

    public GovernmentJobCorrigendum(GovernmentJob job, CorrigendumType noticeType, String title, String documentUrl,
                                    Instant issueDate, String description, Instant revisedLastDate, Integer revisedVacancies) {
        this.job = job;
        this.noticeType = noticeType;
        this.title = title;
        this.documentUrl = documentUrl;
        this.issueDate = issueDate;
        this.description = description;
        this.revisedLastDate = revisedLastDate;
        this.revisedVacancies = revisedVacancies;
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

    public CorrigendumType getNoticeType() { return noticeType; }
    public void setNoticeType(CorrigendumType noticeType) { this.noticeType = noticeType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public Instant getIssueDate() { return issueDate; }
    public void setIssueDate(Instant issueDate) { this.issueDate = issueDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getRevisedLastDate() { return revisedLastDate; }
    public void setRevisedLastDate(Instant revisedLastDate) { this.revisedLastDate = revisedLastDate; }

    public Integer getRevisedVacancies() { return revisedVacancies; }
    public void setRevisedVacancies(Integer revisedVacancies) { this.revisedVacancies = revisedVacancies; }

    public Instant getCreatedAt() { return createdAt; }
}
