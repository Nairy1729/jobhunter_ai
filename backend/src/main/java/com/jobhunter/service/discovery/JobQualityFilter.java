package com.jobhunter.service.discovery;

import com.jobhunter.service.discovery.adapter.JobSourceAdapter;
import com.jobhunter.service.discovery.adapter.JobSourceAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class JobQualityFilter {

    private static final Logger log = LoggerFactory.getLogger(JobQualityFilter.class);

    private static final Set<String> NON_JOB_PATHS = Set.of(
            "/", "/jobs", "/jobs/", "/careers", "/careers/", "/positions", "/openings",
            "/search", "/department", "/teams", "/categories", "/all-jobs"
    );

    private static final Pattern EXPIRED_TEXT_PATTERN = Pattern.compile(
            "(this job posting has expired|no longer accepting applications|this role has been filled|position closed|this job is no longer available|job has been unposted|posting is closed|page not found|404 not found)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GENERIC_PAGE_TITLES = Pattern.compile(
            "^(careers|jobs|current openings|open positions|working at|join our team|search jobs|404|not found)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final UrlNormalizer urlNormalizer;
    private final JobSourceAdapterRegistry adapterRegistry;

    public JobQualityFilter(UrlNormalizer urlNormalizer, JobSourceAdapterRegistry adapterRegistry) {
        this.urlNormalizer = urlNormalizer;
        this.adapterRegistry = adapterRegistry;
    }

    public boolean isPromisingUrl(String url, String title) {
        if (!urlNormalizer.isValidPublicUrl(url)) {
            log.debug("FILTER_REJECT_INVALID_URL - URL is invalid or violates SSRF policy: [{}]", url);
            return false;
        }

        try {
            URI uri = URI.create(url);
            String path = uri.getPath() != null ? uri.getPath().toLowerCase() : "";

            // Check if path is an obvious non-job path
            if (NON_JOB_PATHS.contains(path)) {
                log.debug("FILTER_REJECT_GENERIC_PATH - URL path is generic listing: [{}]", url);
                return false;
            }

            // Check adapter specific validation
            JobSourceAdapter adapter = adapterRegistry.findAdapter(url);
            if (!adapter.isJobPosting(url, title)) {
                log.debug("FILTER_REJECT_NON_JOB - Adapter [{}] rejected URL as non-job: [{}]", adapter.getSourceIdentifier(), url);
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isQualityJobPage(String url, String title, String markdownContent, Integer statusCode) {
        if (statusCode != null && (statusCode == 404 || statusCode == 410 || statusCode == 403 || statusCode >= 500)) {
            log.debug("FILTER_REJECT_STATUS - Page returned HTTP status [{}]: [{}]", statusCode, url);
            return false;
        }

        if (markdownContent == null || markdownContent.trim().length() < 100) {
            log.debug("FILTER_REJECT_EMPTY_CONTENT - Scraped content is too short (<100 chars): [{}]", url);
            return false;
        }

        // Check for closed / expired phrases
        if (EXPIRED_TEXT_PATTERN.matcher(markdownContent).find()) {
            log.debug("FILTER_REJECT_EXPIRED - Scraped content contains expired indicators: [{}]", url);
            return false;
        }

        // Check adapter-specific closed heuristics
        JobSourceAdapter adapter = adapterRegistry.findAdapter(url);
        if (adapter.isJobPostingClosed(markdownContent, statusCode != null ? statusCode : 200)) {
            log.debug("FILTER_REJECT_ADAPTER_CLOSED - Adapter [{}] flagged page as closed: [{}]", adapter.getSourceIdentifier(), url);
            return false;
        }

        // Check generic title rejection
        if (title != null && GENERIC_PAGE_TITLES.matcher(title.trim()).find()) {
            log.debug("FILTER_REJECT_GENERIC_TITLE - Page title appears to be a generic portal title: [{}]", title);
            return false;
        }

        // Minimum job signal check: page must contain at least one job description indicator
        String lower = markdownContent.toLowerCase();
        boolean hasSignal = lower.contains("responsibilities") ||
                lower.contains("requirements") ||
                lower.contains("qualifications") ||
                lower.contains("about the role") ||
                lower.contains("about the job") ||
                lower.contains("experience") ||
                lower.contains("skills") ||
                lower.contains("what you'll do") ||
                lower.contains("who you are");

        if (!hasSignal) {
            log.debug("FILTER_REJECT_NO_JOB_SIGNALS - Page does not contain standard job description sections: [{}]", url);
            return false;
        }

        return true;
    }
}
