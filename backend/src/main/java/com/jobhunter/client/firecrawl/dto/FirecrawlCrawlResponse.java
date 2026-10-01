package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlCrawlResponse {

    private boolean success = true;
    private String id;
    private String url;
    private String error;

    public FirecrawlCrawlResponse() {}

    public FirecrawlCrawlResponse(boolean success, String id, String url) {
        this.success = success;
        this.id = id;
        this.url = url;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
