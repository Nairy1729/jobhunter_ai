package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlMapResponse {

    private boolean success = true;
    private List<String> links = new ArrayList<>();
    private String error;

    public FirecrawlMapResponse() {}

    public FirecrawlMapResponse(boolean success, List<String> links) {
        this.success = success;
        this.links = links != null ? links : new ArrayList<>();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<String> getLinks() { return links; }
    public void setLinks(List<String> links) { this.links = links != null ? links : new ArrayList<>(); }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
