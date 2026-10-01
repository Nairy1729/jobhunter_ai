package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlDocument {

    private String url;
    private String title;
    private String description;
    private String markdown;
    private FirecrawlMetadata metadata;

    public FirecrawlDocument() {}

    public FirecrawlDocument(String url, String title, String markdown) {
        this.url = url;
        this.title = title;
        this.markdown = markdown;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }

    public FirecrawlMetadata getMetadata() { return metadata; }
    public void setMetadata(FirecrawlMetadata metadata) { this.metadata = metadata; }
}
