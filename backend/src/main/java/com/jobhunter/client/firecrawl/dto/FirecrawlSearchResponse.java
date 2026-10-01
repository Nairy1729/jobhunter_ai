package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlSearchResponse {

    private boolean success = true;
    private List<FirecrawlDocument> data = new ArrayList<>();
    private String error;

    public FirecrawlSearchResponse() {}

    public FirecrawlSearchResponse(boolean success, List<FirecrawlDocument> data) {
        this.success = success;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<FirecrawlDocument> getData() { return data; }
    public void setData(List<FirecrawlDocument> data) { this.data = data != null ? data : new ArrayList<>(); }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
