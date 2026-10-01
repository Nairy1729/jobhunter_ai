package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String domain;

    @Column(name = "career_page_url", length = 500)
    private String careerPageUrl;

    @Column(name = "ats_provider", length = 50)
    private String atsProvider; // GREENHOUSE, LEVER, WORKDAY, ASHBY, CUSTOM

    @Column(length = 100)
    private String industry;

    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(length = 150)
    private String headquarters;

    @Column(name = "known_tech_stack", columnDefinition = "jsonb", nullable = false)
    private String knownTechStack = "[]";

    @Column(name = "intelligence_summary", columnDefinition = "jsonb", nullable = false)
    private String intelligenceSummary = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Company() {}

    public Company(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getCareerPageUrl() { return careerPageUrl; }
    public void setCareerPageUrl(String careerPageUrl) { this.careerPageUrl = careerPageUrl; }

    public String getAtsProvider() { return atsProvider; }
    public void setAtsProvider(String atsProvider) { this.atsProvider = atsProvider; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }

    public String getHeadquarters() { return headquarters; }
    public void setHeadquarters(String headquarters) { this.headquarters = headquarters; }

    public String getKnownTechStack() { return knownTechStack; }
    public void setKnownTechStack(String knownTechStack) { this.knownTechStack = knownTechStack; }

    public String getIntelligenceSummary() { return intelligenceSummary; }
    public void setIntelligenceSummary(String intelligenceSummary) { this.intelligenceSummary = intelligenceSummary; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
