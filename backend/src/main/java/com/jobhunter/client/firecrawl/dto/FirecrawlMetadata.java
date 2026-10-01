package com.jobhunter.client.firecrawl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlMetadata {

    private String title;
    private String description;

    @JsonProperty("sourceURL")
    private String sourceUrl;

    private String canonical;

    @JsonProperty("statusCode")
    private Integer statusCode;

    private String datePosted;
    private String ogTitle;
    private String ogDescription;
    private String language;

    public FirecrawlMetadata() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getCanonical() { return canonical; }
    public void setCanonical(String canonical) { this.canonical = canonical; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getDatePosted() { return datePosted; }
    public void setDatePosted(String datePosted) { this.datePosted = datePosted; }

    public String getOgTitle() { return ogTitle; }
    public void setOgTitle(String ogTitle) { this.ogTitle = ogTitle; }

    public String getOgDescription() { return ogDescription; }
    public void setOgDescription(String ogDescription) { this.ogDescription = ogDescription; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
