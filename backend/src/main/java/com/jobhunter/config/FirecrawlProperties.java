package com.jobhunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "firecrawl")
public class FirecrawlProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.firecrawl.dev";
    private int connectionTimeoutSeconds = 15;
    private int readTimeoutSeconds = 30;

    public FirecrawlProperties() {}

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey.trim() : ""; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://api.firecrawl.dev"; }

    public int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) { this.connectionTimeoutSeconds = connectionTimeoutSeconds; }

    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
