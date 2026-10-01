package com.jobhunter.service.discovery.adapter;

import com.jobhunter.service.discovery.UrlNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkdayAdapter implements JobSourceAdapter {

    private static final Pattern WORKDAY_JOB_PATTERN = Pattern.compile(
            "https?://([^.]+)\\.(?:wd\\d+\\.)?myworkdayjobs\\.com/(?:[a-zA-Z\\-]+/)?([^/]+)/job/(?:[^/]+/)?([^/?#]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WORKDAY_HOST_PATTERN = Pattern.compile(
            "https?://([^.]+)\\.(?:wd\\d+\\.)?myworkdayjobs\\.com",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CLOSED_PATTERN = Pattern.compile(
            "(this job posting has expired|no longer accepting applications|this (?:role|position|job) has been filled|position closed|this job is no longer available)",
            Pattern.CASE_INSENSITIVE
    );

    private final UrlNormalizer urlNormalizer;

    public WorkdayAdapter(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public String getSourceIdentifier() {
        return "WORKDAY";
    }

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        return url.toLowerCase().contains("myworkdayjobs.com");
    }

    @Override
    public String canonicalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";

        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath();

            if (path != null && path.contains("/job/")) {
                // Return clean normalized path without query tracking or fragments
                return "https://" + host + path;
            }
        } catch (Exception ignored) {}

        return urlNormalizer.normalizeUrl(rawUrl);
    }

    @Override
    public String extractCompany(String url, String pageTitle) {
        if (url != null) {
            Matcher m = WORKDAY_HOST_PATTERN.matcher(url);
            if (m.find()) {
                String tenant = m.group(1).replace("-", " ").trim();
                return capitalizeWords(tenant);
            }
        }
        if (pageTitle != null && pageTitle.contains(" at ")) {
            return pageTitle.substring(pageTitle.lastIndexOf(" at ") + 4).trim();
        }
        return "Unknown Company";
    }

    @Override
    public boolean isJobPosting(String url, String pageTitle) {
        if (url == null) return false;
        return url.toLowerCase().contains("/job/");
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
