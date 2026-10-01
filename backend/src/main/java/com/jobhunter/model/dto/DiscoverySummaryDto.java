package com.jobhunter.model.dto;

import java.util.ArrayList;
import java.util.List;

public class DiscoverySummaryDto {

    private int queriesExecuted;
    private int searchResultsFound;
    private int pagesScraped;
    private int jobsCreated;
    private int duplicatesSkipped;
    private int invalidPagesSkipped;
    private List<String> errors = new ArrayList<>();
    private List<JobDto> jobs = new ArrayList<>();
    private long executionTimeMs;

    public DiscoverySummaryDto() {}

    public int getQueriesExecuted() { return queriesExecuted; }
    public void setQueriesExecuted(int queriesExecuted) { this.queriesExecuted = queriesExecuted; }

    public int getSearchResultsFound() { return searchResultsFound; }
    public void setSearchResultsFound(int searchResultsFound) { this.searchResultsFound = searchResultsFound; }

    public int getPagesScraped() { return pagesScraped; }
    public void setPagesScraped(int pagesScraped) { this.pagesScraped = pagesScraped; }

    public int getJobsCreated() { return jobsCreated; }
    public void setJobsCreated(int jobsCreated) { this.jobsCreated = jobsCreated; }

    public int getDuplicatesSkipped() { return duplicatesSkipped; }
    public void setDuplicatesSkipped(int duplicatesSkipped) { this.duplicatesSkipped = duplicatesSkipped; }

    public int getInvalidPagesSkipped() { return invalidPagesSkipped; }
    public void setInvalidPagesSkipped(int invalidPagesSkipped) { this.invalidPagesSkipped = invalidPagesSkipped; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<JobDto> getJobs() { return jobs; }
    public void setJobs(List<JobDto> jobs) { this.jobs = jobs; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
