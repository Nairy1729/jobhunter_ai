package com.jobhunter.model.dto;

import java.util.List;

public class DiscoveryRequest {

    private Integer maxQueries = 4;
    private Integer searchLimitPerQuery = 10;
    private List<String> sources; // Optional: ["GREENHOUSE", "LEVER", "WORKDAY", "ASHBY"]

    public DiscoveryRequest() {}

    public DiscoveryRequest(Integer maxQueries, Integer searchLimitPerQuery) {
        this.maxQueries = maxQueries;
        this.searchLimitPerQuery = searchLimitPerQuery;
    }

    public Integer getMaxQueries() { return maxQueries; }
    public void setMaxQueries(Integer maxQueries) { this.maxQueries = maxQueries; }

    public Integer getSearchLimitPerQuery() { return searchLimitPerQuery; }
    public void setSearchLimitPerQuery(Integer searchLimitPerQuery) { this.searchLimitPerQuery = searchLimitPerQuery; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
}
