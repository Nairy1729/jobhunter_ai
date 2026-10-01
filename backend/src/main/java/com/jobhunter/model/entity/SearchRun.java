package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_runs")
public class SearchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_source_id", nullable = false)
    private JobSource jobSource;

    @Column(name = "query_string", columnDefinition = "TEXT", nullable = false)
    private String queryString;

    @Column(name = "parameters", columnDefinition = "jsonb", nullable = false)
    private String parameters = "{}";

    @Column(nullable = false, length = 50)
    private String status = "RUNNING"; // RUNNING, COMPLETED, FAILED

    @Column(name = "jobs_discovered_count", nullable = false)
    private Integer jobsDiscoveredCount = 0;

    @Column(name = "jobs_ingested_count", nullable = false)
    private Integer jobsIngestedCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public SearchRun() {}

    public SearchRun(JobSource jobSource, String queryString, String parameters) {
        this.jobSource = jobSource;
        this.queryString = queryString;
        this.parameters = parameters != null ? parameters : "{}";
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public JobSource getJobSource() { return jobSource; }
    public void setJobSource(JobSource jobSource) { this.jobSource = jobSource; }

    public String getQueryString() { return queryString; }
    public void setQueryString(String queryString) { this.queryString = queryString; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getJobsDiscoveredCount() { return jobsDiscoveredCount; }
    public void setJobsDiscoveredCount(Integer jobsDiscoveredCount) { this.jobsDiscoveredCount = jobsDiscoveredCount; }

    public Integer getJobsIngestedCount() { return jobsIngestedCount; }
    public void setJobsIngestedCount(Integer jobsIngestedCount) { this.jobsIngestedCount = jobsIngestedCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
