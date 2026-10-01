package com.jobhunter.service.discovery.adapter;

import com.jobhunter.service.discovery.UrlNormalizer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GreenhouseAdapter implements JobSourceAdapter {

    private static final Pattern GREENHOUSE_PATH_PATTERN = Pattern.compile(
            "https?://(?:boards|job-boards)\\.greenhouse\\.io/([^/]+)/jobs/(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GREENHOUSE_EMBED_PATTERN = Pattern.compile(
            "https?://(?:boards|job-boards)\\.greenhouse\\.io/embed/job_app\\?.*token=(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CLOSED_PATTERN = Pattern.compile(
            "(this job posting has expired|no longer accepting applications|this (?:role|position|job) has been filled|position closed|this job is no longer available)",
            Pattern.CASE_INSENSITIVE
    );

    private final UrlNormalizer urlNormalizer;

    public GreenhouseAdapter(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public String getSourceIdentifier() {
        return "GREENHOUSE";
    }

    @Override
    public boolean supports(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("boards.greenhouse.io") || lower.contains("job-boards.greenhouse.io");
    }

    @Override
    public String canonicalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";

        Matcher pathMatcher = GREENHOUSE_PATH_PATTERN.matcher(rawUrl);
        if (pathMatcher.find()) {
            String company = pathMatcher.group(1).toLowerCase();
            String jobId = pathMatcher.group(2);
            return "https://boards.greenhouse.io/" + company + "/jobs/" + jobId;
        }

        // Check embed format: /embed/job_app?for={company}&token={token}
        if (rawUrl.contains("/embed/job_app")) {
            try {
                URI uri = URI.create(rawUrl);
                String query = uri.getQuery();
                if (query != null) {
                    String company = null;
                    String token = null;
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=");
                        if (kv.length == 2) {
                            if (kv[0].equalsIgnoreCase("for")) company = kv[1].toLowerCase();
                            if (kv[0].equalsIgnoreCase("token")) token = kv[1];
                        }
                    }
                    if (company != null && token != null) {
                        return "https://boards.greenhouse.io/" + company + "/jobs/" + token;
                    }
                }
            } catch (Exception ignored) {}
        }

        return urlNormalizer.normalizeUrl(rawUrl);
    }

    @Override
    public String extractCompany(String url, String pageTitle) {
        if (url != null) {
            Matcher m = GREENHOUSE_PATH_PATTERN.matcher(url);
            if (m.find()) {
                String comp = m.group(1).replace("-", " ").trim();
                return capitalizeWords(comp);
            }
        }
        if (pageTitle != null) {
            if (pageTitle.contains(" at ")) {
                return pageTitle.substring(pageTitle.lastIndexOf(" at ") + 4).trim();
            }
            if (pageTitle.contains(" - ")) {
                String[] parts = pageTitle.split(" - ");
                if (parts.length > 1) {
                    return parts[parts.length - 1].trim();
                }
            }
        }
        return "Unknown Company";
    }

    @Override
    public boolean isJobPosting(String url, String pageTitle) {
        if (url == null) return false;
        if (GREENHOUSE_PATH_PATTERN.matcher(url).find()) return true;
        if (GREENHOUSE_EMBED_PATTERN.matcher(url).find()) return true;
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
