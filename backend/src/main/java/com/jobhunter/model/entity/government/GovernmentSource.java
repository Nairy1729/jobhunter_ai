package com.jobhunter.model.entity.government;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "government_sources")
public class GovernmentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "official_domain", nullable = false, length = 255)
    private String officialDomain;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 60)
    private GovernmentSourceType sourceType;

    @Column(length = 255)
    private String department;

    @Column(length = 255)
    private String authority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GovernmentSourcePriority priority = GovernmentSourcePriority.TIER_1_OFFICIAL;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_frequency", nullable = false, length = 40)
    private CrawlFrequency crawlFrequency = CrawlFrequency.DAILY;

    @Column(name = "last_checked")
    private Instant lastChecked;

    @Column(name = "last_successful_crawl")
    private Instant lastSuccessfulCrawl;

    @Column(name = "last_attempt")
    private Instant lastAttempt;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public GovernmentSource() {}

    public GovernmentSource(String name, String url, String officialDomain, String state, String district,
                            GovernmentSourceType sourceType, String department, String authority,
                            GovernmentSourcePriority priority, CrawlFrequency crawlFrequency) {
        this.name = name;
        this.url = url;
        this.officialDomain = officialDomain;
        this.state = state;
        this.district = district;
        this.sourceType = sourceType;
        this.department = department;
        this.authority = authority;
        this.priority = priority;
        this.crawlFrequency = crawlFrequency;
        this.active = true;
        this.failureCount = 0;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

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

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
