package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirecrawlScrapeRequest {

    private String url;
    private List<String> formats = new ArrayList<>(List.of("markdown"));
    private Boolean onlyMainContent = true;
    private Integer waitFor = 1500;

    public FirecrawlScrapeRequest() {}

    public FirecrawlScrapeRequest(String url) {
        this.url = url;
    }

    public FirecrawlScrapeRequest(String url, Integer waitFor) {
        this.url = url;
        this.waitFor = waitFor;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<String> getFormats() { return formats; }
    public void setFormats(List<String> formats) { this.formats = formats; }

    public Boolean getOnlyMainContent() { return onlyMainContent; }
    public void setOnlyMainContent(Boolean onlyMainContent) { this.onlyMainContent = onlyMainContent; }

    public Integer getWaitFor() { return waitFor; }
    public void setWaitFor(Integer waitFor) { this.waitFor = waitFor; }
}
