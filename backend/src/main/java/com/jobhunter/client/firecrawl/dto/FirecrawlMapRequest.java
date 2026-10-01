package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirecrawlMapRequest {

    private String url;
    private String search;

    public FirecrawlMapRequest() {}

    public FirecrawlMapRequest(String url) {
        this.url = url;
    }

    public FirecrawlMapRequest(String url, String search) {
        this.url = url;
        this.search = search;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
}
