package com.jobhunter.client.firecrawl.exception;

public class FirecrawlApiKeyMissingException extends FirecrawlClientException {
    public FirecrawlApiKeyMissingException() {
        super("Firecrawl API key is missing or not configured. Please set the FIRECRAWL_API_KEY environment variable.", 401);
    }

    public FirecrawlApiKeyMissingException(String message) {
        super(message, 401);
    }
}
