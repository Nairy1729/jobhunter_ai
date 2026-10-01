package com.jobhunter.service.discovery.adapter;

public interface JobSourceAdapter {

    String getSourceIdentifier();

    boolean supports(String url);

    String canonicalizeUrl(String rawUrl);

    String extractCompany(String url, String pageTitle);

    boolean isJobPosting(String url, String pageTitle);

    boolean isJobPostingClosed(String pageContent, int httpStatusCode);
}
