package com.jobhunter.model.dto.government;

import com.jobhunter.model.entity.government.CrawlFrequency;
import com.jobhunter.model.entity.government.GovernmentSourcePriority;
import com.jobhunter.model.entity.government.GovernmentSourceType;

import java.time.Instant;
import java.util.UUID;

public class GovernmentSourceDto {
    private UUID id;
    private String name;
    private String url;
    private String officialDomain;
    private String state;
    private String district;
    private GovernmentSourceType sourceType;
    private String department;
    private String authority;
    private GovernmentSourcePriority priority;
    private boolean active;
    private CrawlFrequency crawlFrequency;
    private Instant lastChecked;
    private Instant lastSuccessfulCrawl;
    private Instant lastAttempt;
    private int failureCount;
    private String status;

    public GovernmentSourceDto() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getOfficialDomain() { return officialDomain; }
    public void setOfficialDomain(String officialDomain) { this.officialDomain = officialDomain; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public GovernmentSourceType getSourceType() { return sourceType; }
    public void setSourceType(GovernmentSourceType sourceType) { this.sourceType = sourceType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAuthority() { return authority; }
    public void setAuthority(String authority) { this.authority = authority; }
    public GovernmentSourcePriority getPriority() { return priority; }
    public void setPriority(GovernmentSourcePriority priority) { this.priority = priority; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public CrawlFrequency getCrawlFrequency() { return crawlFrequency; }
    public void setCrawlFrequency(CrawlFrequency crawlFrequency) { this.crawlFrequency = crawlFrequency; }
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }
    public Instant getLastSuccessfulCrawl() { return lastSuccessfulCrawl; }
    public void setLastSuccessfulCrawl(Instant lastSuccessfulCrawl) { this.lastSuccessfulCrawl = lastSuccessfulCrawl; }
    public Instant getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
