package com.jobhunter.client.firecrawl.exception;

public class FirecrawlClientException extends RuntimeException {
    private final int statusCode;

    public FirecrawlClientException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public FirecrawlClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public FirecrawlClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public FirecrawlClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
