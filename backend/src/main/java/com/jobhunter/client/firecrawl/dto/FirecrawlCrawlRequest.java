package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirecrawlCrawlRequest {

    private String url;
    private Integer limit = 50;
    private Integer maxDepth = 2;
    private FirecrawlScrapeOptions scrapeOptions;

    public FirecrawlCrawlRequest() {}

    public FirecrawlCrawlRequest(String url, Integer limit, Integer maxDepth) {
        this.url = url;
        this.limit = limit;
        this.maxDepth = maxDepth;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Integer getMaxDepth() { return maxDepth; }
    public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }

    public FirecrawlScrapeOptions getScrapeOptions() { return scrapeOptions; }
    public void setScrapeOptions(FirecrawlScrapeOptions scrapeOptions) { this.scrapeOptions = scrapeOptions; }
}
