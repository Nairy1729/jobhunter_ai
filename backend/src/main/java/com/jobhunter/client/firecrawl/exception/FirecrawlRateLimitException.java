package com.jobhunter.client.firecrawl.exception;

public class FirecrawlRateLimitException extends FirecrawlClientException {
    public FirecrawlRateLimitException(String message) {
        super(message, 429);
    }

    public FirecrawlRateLimitException(String message, Throwable cause) {
        super(message, 429, cause);
    }
}
