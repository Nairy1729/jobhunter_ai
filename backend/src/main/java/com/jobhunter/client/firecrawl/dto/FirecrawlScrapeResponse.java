package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlScrapeResponse {

    private boolean success = true;
    private FirecrawlDocument data;
    private String error;

    public FirecrawlScrapeResponse() {}

    public FirecrawlScrapeResponse(boolean success, FirecrawlDocument data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public FirecrawlDocument getData() { return data; }
    public void setData(FirecrawlDocument data) { this.data = data; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
