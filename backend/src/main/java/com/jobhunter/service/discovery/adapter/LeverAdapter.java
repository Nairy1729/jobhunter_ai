package com.jobhunter.service.discovery.adapter;

import com.jobhunter.service.discovery.UrlNormalizer;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LeverAdapter implements JobSourceAdapter {

    private static final Pattern LEVER_POSTING_PATTERN = Pattern.compile(
            "https?://jobs\\.lever\\.co/([^/]+)/([a-f0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CLOSED_PATTERN = Pattern.compile(
            "(this job posting has expired|no longer accepting applications|this (?:role|position|job) has been filled|position closed|this job is no longer available)",
            Pattern.CASE_INSENSITIVE
    );

    private final UrlNormalizer urlNormalizer;

    public LeverAdapter(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public String getSourceIdentifier() {
        return "LEVER";
    }

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return url.toLowerCase().contains("jobs.lever.co");
    }

    @Override
    public String canonicalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";

        Matcher matcher = LEVER_POSTING_PATTERN.matcher(rawUrl);
        if (matcher.find()) {
            String company = matcher.group(1).toLowerCase();
            String jobId = matcher.group(2).toLowerCase();
            return "https://jobs.lever.co/" + company + "/" + jobId;
        }

        return urlNormalizer.normalizeUrl(rawUrl);
    }

    @Override
    public String extractCompany(String url, String pageTitle) {
        if (url != null) {
            Matcher m = LEVER_POSTING_PATTERN.matcher(url);
            if (m.find()) {
                String comp = m.group(1).replace("-", " ").trim();
                return capitalizeWords(comp);
            }
        }
        if (pageTitle != null && pageTitle.contains(" - ")) {
            String[] parts = pageTitle.split(" - ");
            return parts[parts.length - 1].trim();
        }
        return "Unknown Company";
    }

    @Override
    public boolean isJobPosting(String url, String pageTitle) {
        if (url == null) return false;
        return LEVER_POSTING_PATTERN.matcher(url).find();
    }

    @Override
    public boolean isJobPostingClosed(String pageContent, int httpStatusCode) {
        if (httpStatusCode == 404 || httpStatusCode == 410) return true;
        if (pageContent != null && CLOSED_PATTERN.matcher(pageContent).find()) return true;
        return false;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isBlank()) return text;
        StringBuilder sb = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
