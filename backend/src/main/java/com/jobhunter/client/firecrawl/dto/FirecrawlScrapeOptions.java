package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirecrawlScrapeOptions {

    private List<String> formats = new ArrayList<>(List.of("markdown"));
    private Boolean onlyMainContent = true;
    private Integer waitFor = 1500;

    public FirecrawlScrapeOptions() {}

    public FirecrawlScrapeOptions(List<String> formats, Boolean onlyMainContent, Integer waitFor) {
        this.formats = formats;
        this.onlyMainContent = onlyMainContent;
        this.waitFor = waitFor;
    }

    public List<String> getFormats() { return formats; }
    public void setFormats(List<String> formats) { this.formats = formats; }

    public Boolean getOnlyMainContent() { return onlyMainContent; }
    public void setOnlyMainContent(Boolean onlyMainContent) { this.onlyMainContent = onlyMainContent; }

    public Integer getWaitFor() { return waitFor; }
    public void setWaitFor(Integer waitFor) { this.waitFor = waitFor; }
}
