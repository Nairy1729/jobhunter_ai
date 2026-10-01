package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirecrawlSearchRequest {

    private String query;
    private Integer limit = 10;
    private FirecrawlScrapeOptions scrapeOptions;

    public FirecrawlSearchRequest() {}

    public FirecrawlSearchRequest(String query, Integer limit) {
        this.query = query;
        this.limit = limit;
        this.scrapeOptions = new FirecrawlScrapeOptions();
    }

    public FirecrawlSearchRequest(String query, Integer limit, FirecrawlScrapeOptions scrapeOptions) {
        this.query = query;
        this.limit = limit;
        this.scrapeOptions = scrapeOptions;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public FirecrawlScrapeOptions getScrapeOptions() { return scrapeOptions; }
    public void setScrapeOptions(FirecrawlScrapeOptions scrapeOptions) { this.scrapeOptions = scrapeOptions; }
}
