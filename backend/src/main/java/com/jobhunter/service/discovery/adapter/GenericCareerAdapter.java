package com.jobhunter.service.discovery.adapter;

import com.jobhunter.service.discovery.UrlNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenericCareerAdapter implements JobSourceAdapter {

    private static final Pattern CLOSED_PATTERN = Pattern.compile(
            "(this job posting has expired|no longer accepting applications|this role has been filled|position closed|this job is no longer available|page not found|404 not found)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JOB_PATH_INDICATOR = Pattern.compile(
            "/(?:jobs?|careers?|positions?|openings?|posting)/[^/?#]+",
            Pattern.CASE_INSENSITIVE
    );

    private final UrlNormalizer urlNormalizer;

    public GenericCareerAdapter(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public String getSourceIdentifier() {
        return "GENERIC";
    }

    @Override
    public boolean supports(String url) {
        return true; // Fallback adapter supports all URLs
    }

    @Override
    public String canonicalizeUrl(String rawUrl) {
        return urlNormalizer.normalizeUrl(rawUrl);
    }

    @Override
    public String extractCompany(String url, String pageTitle) {
        if (pageTitle != null && !pageTitle.isBlank()) {
            if (pageTitle.contains(" at ")) {
                return pageTitle.substring(pageTitle.lastIndexOf(" at ") + 4).trim();
            }
            if (pageTitle.contains(" - ")) {
                String[] parts = pageTitle.split(" - ");
                if (parts.length > 1) {
                    return parts[parts.length - 1].trim();
                }
            }
            if (pageTitle.contains(" | ")) {
                String[] parts = pageTitle.split(" \\| ");
                if (parts.length > 1) {
                    return parts[parts.length - 1].trim();
                }
            }
        }

        if (url != null) {
            try {
                URI uri = URI.create(url);
                String host = uri.getHost();
                if (host != null) {
                    String[] parts = host.split("\\.");
                    if (parts.length >= 2) {
                        String domainName = parts[parts.length - 2];
                        return capitalizeWords(domainName);
                    }
                }
            } catch (Exception ignored) {}
        }

        return "Unknown Company";
    }

    @Override
    public boolean isJobPosting(String url, String pageTitle) {
        if (url == null || url.isBlank()) return false;

        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.equals("/") || path.isBlank()) {
                return false; // Root homepage is not a job posting
            }

            // Must have deeper path or job indicator
            if (JOB_PATH_INDICATOR.matcher(path).find()) {
                return true;
            }

            // If title indicates specific job
            if (pageTitle != null) {
                String lowerTitle = pageTitle.toLowerCase();
                if (lowerTitle.contains("engineer") || lowerTitle.contains("developer") ||
                    lowerTitle.contains("architect") || lowerTitle.contains("consultant")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
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
