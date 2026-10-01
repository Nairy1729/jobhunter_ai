package com.jobhunter.model.dto;

import java.time.Instant;
import java.util.UUID;

public class SearchRunDto {

    private UUID id;
    private String jobSourceName;
    private String queryString;
    private String status;
    private Integer jobsDiscoveredCount;
    private Integer jobsIngestedCount;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    public SearchRunDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getJobSourceName() { return jobSourceName; }
    public void setJobSourceName(String jobSourceName) { this.jobSourceName = jobSourceName; }

    public String getQueryString() { return queryString; }
    public void setQueryString(String queryString) { this.queryString = queryString; }

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
