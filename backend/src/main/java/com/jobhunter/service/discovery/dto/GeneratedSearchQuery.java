package com.jobhunter.service.discovery.dto;

import java.util.List;

public class GeneratedSearchQuery {

    private String queryString;
    private String sourceIdentifier;
    private List<String> roles;
    private List<String> skills;
    private List<String> locations;
    private String strategyTier = "EXACT"; // EXACT, ADJACENT, SKILL_LED

    public GeneratedSearchQuery() {}

    public GeneratedSearchQuery(String queryString, String sourceIdentifier, List<String> roles, List<String> skills, List<String> locations) {
        this(queryString, sourceIdentifier, roles, skills, locations, "EXACT");
    }

    public GeneratedSearchQuery(String queryString, String sourceIdentifier, List<String> roles, List<String> skills, List<String> locations, String strategyTier) {
        this.queryString = queryString;
        this.sourceIdentifier = sourceIdentifier;
        this.roles = roles;
        this.skills = skills;
        this.locations = locations;
        this.strategyTier = strategyTier;
    }

    public String getStrategyTier() { return strategyTier; }
    public void setStrategyTier(String strategyTier) { this.strategyTier = strategyTier; }

    public String getQueryString() { return queryString; }
    public void setQueryString(String queryString) { this.queryString = queryString; }

    public String getSourceIdentifier() { return sourceIdentifier; }
    public void setSourceIdentifier(String sourceIdentifier) { this.sourceIdentifier = sourceIdentifier; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }
}
