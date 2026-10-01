package com.jobhunter.service.discovery;

import com.jobhunter.client.firecrawl.dto.FirecrawlDocument;
import com.jobhunter.client.firecrawl.dto.FirecrawlMetadata;
import com.jobhunter.model.entity.Skill;
import com.jobhunter.repository.SkillRepository;
import com.jobhunter.service.discovery.adapter.JobSourceAdapter;
import com.jobhunter.service.discovery.dto.ExtractedJobDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobExtractorService {

    private static final Logger log = LoggerFactory.getLogger(JobExtractorService.class);

    private static final Pattern SALARY_USD_PATTERN = Pattern.compile(
            "\\$\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{2,6})\\s*(?:-|to)\\s*\\$\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{2,6})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SALARY_INR_LPA_PATTERN = Pattern.compile(
            "(?:₹|INR)?\\s*([0-9]{1,2}(?:\\.[0-9]{1,2})?)\\s*(?:-|to)\\s*([0-9]{1,2}(?:\\.[0-9]{1,2})?)\\s*(?:LPA|Lakhs?|Lac)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SALARY_INR_EXPLICIT_PATTERN = Pattern.compile(
            "₹\\s*([0-9]{1,3}(?:,[0-9]{2,3})+)\\s*(?:-|to)\\s*₹?\\s*([0-9]{1,3}(?:,[0-9]{2,3})+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPERIENCE_RANGE_PATTERN = Pattern.compile(
            "\\b([0-9]+)\\s*(?:-|to)\\s*([0-9]+)\\s*(?:\\+?\\s*years?|\\+?\\s*yrs?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPERIENCE_SINGLE_PATTERN = Pattern.compile(
            "\\b([0-9]+)\\+?\\s*(?:years?|yrs?)(?:\\s+of)?\\s+(?:relevant|commercial|hands-on)?\\s*experience\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(202[4-9]-[0-1][0-9]-[0-3][0-9])\\b"
    );

    private final SkillRepository skillRepository;
    private final DeduplicationService deduplicationService;

    // Cache of taxonomy skill names for fast in-memory matching
    private List<Skill> cachedSkills = null;

    public JobExtractorService(SkillRepository skillRepository, DeduplicationService deduplicationService) {
        this.skillRepository = skillRepository;
        this.deduplicationService = deduplicationService;
    }

    public ExtractedJobDetails extractJobDetails(FirecrawlDocument doc, JobSourceAdapter adapter) {
        ExtractedJobDetails details = new ExtractedJobDetails();

        String rawUrl = doc.getUrl() != null ? doc.getUrl() : "";
        String canonicalUrl = adapter.canonicalizeUrl(rawUrl);
        details.setJobUrl(rawUrl);
        details.setCanonicalUrl(canonicalUrl);

        String markdown = doc.getMarkdown() != null ? doc.getMarkdown() : "";
        details.setRawDescriptionMarkdown(markdown);

        FirecrawlMetadata meta = doc.getMetadata();

        // 1. Title Extraction
        String title = extractTitle(doc, meta, markdown);
        details.setTitle(title);
        details.setNormalizedTitle(normalizeJobTitle(title));

        // 2. Company Extraction
        String company = adapter.extractCompany(rawUrl, title);
        if (company.equalsIgnoreCase("Unknown Company") && meta != null && meta.getOgTitle() != null) {
            company = adapter.extractCompany(rawUrl, meta.getOgTitle());
        }
        details.setCompanyName(company);

        // 3. Location Extraction
        String location = extractLocation(markdown, meta);
        details.setLocation(location);

        // 4. Work Mode
        String workMode = detectWorkMode(markdown, location, title);
        details.setWorkMode(workMode);

        // 5. Employment Type
        details.setEmploymentType(detectEmploymentType(markdown));

        // 6. Experience (Strict: null if missing)
        extractExperience(markdown, details);

        // 7. Salary (Strict: null if missing)
        extractSalary(markdown, details);

        // 8. Posting Date (Strict: null if missing)
        details.setPostingDate(extractPostingDate(meta, markdown));

        // 9. Detected Technologies from Skill Taxonomy
        details.setDetectedTechnologies(detectSkills(markdown));

        // 10. Extract Sections
        extractSections(markdown, details);

        // 11. Deduplication Hashes
        String canonicalHash = deduplicationService.generateCanonicalUrlHash(canonicalUrl);
        details.setCanonicalUrlHash(canonicalHash);

        String contentHash = deduplicationService.generateContentHash(details.getTitle(), details.getCompanyName(), markdown);
        details.setContentHash(contentHash);

        log.debug("JOB_EXTRACTED - Title: [{}], Company: [{}], Location: [{}], WorkMode: [{}], TechCount: [{}]",
                details.getTitle(), details.getCompanyName(), details.getLocation(), details.getWorkMode(), details.getDetectedTechnologies().size());

        return details;
    }

    private String extractTitle(FirecrawlDocument doc, FirecrawlMetadata meta, String markdown) {
        String title = null;

        // Try first H1 from markdown
        if (markdown != null && !markdown.isBlank()) {
            for (String line : markdown.split("\\r?\\n")) {
                line = line.trim();
                if (line.startsWith("# ") && line.length() > 2) {
                    title = line.substring(2).trim();
                    break;
                }
            }
        }

        if (title == null || title.isBlank()) {
            if (meta != null && meta.getTitle() != null && !meta.getTitle().isBlank()) {
                title = meta.getTitle();
            } else if (doc.getTitle() != null && !doc.getTitle().isBlank()) {
                title = doc.getTitle();
            }
        }

        if (title == null || title.isBlank()) {
            title = "Software Engineer";
        }

        // Clean title from common suffixes
        title = title.replaceAll("(?i)\\s*\\|\\s*(?:Lever|Greenhouse|Ashby|Workday).*", "");
        title = title.replaceAll("(?i)\\s*-\\s*(?:Careers|Jobs|Job Application).*", "");
        title = title.replaceAll("(?i)\\s*at\\s+[A-Za-z0-9\\s\\.\\-]+$", "");

        return title.trim();
    }

    private String normalizeJobTitle(String title) {
        if (title == null) return "software engineer";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractLocation(String markdown, FirecrawlMetadata meta) {
        if (markdown != null) {
            Matcher m = Pattern.compile("(?i)(?:location|office|place):\\s*([^\\n\\r|]+)").matcher(markdown);
            if (m.find()) {
                String loc = m.group(1).trim();
                if (loc.length() < 60 && !loc.toLowerCase().contains("http")) {
                    return loc;
                }
            }
        }

        // Common Tech Cities check
        if (markdown != null) {
            String lower = markdown.toLowerCase();
            if (lower.contains("bangalore") || lower.contains("bengaluru")) return "Bangalore, India";
            if (lower.contains("hyderabad")) return "Hyderabad, India";
            if (lower.contains("pune")) return "Pune, India";
            if (lower.contains("mumbai")) return "Mumbai, India";
            if (lower.contains("delhi") || lower.contains("gurgaon") || lower.contains("noida")) return "Delhi NCR, India";
            if (lower.contains("san francisco")) return "San Francisco, CA";
            if (lower.contains("new york")) return "New York, NY";
            if (lower.contains("remote")) return "Remote";
        }

        return "Remote / Flexible";
    }

    private String detectWorkMode(String markdown, String location, String title) {
        String combined = (title + " " + location + " " + (markdown != null ? markdown.substring(0, Math.min(markdown.length(), 2000)) : "")).toLowerCase();

        if (combined.contains("remote")) {
            return "REMOTE";
        }
        if (combined.contains("hybrid")) {
            return "HYBRID";
        }
        if (combined.contains("on-site") || combined.contains("onsite") || combined.contains("in-office")) {
            return "ON_SITE";
        }
        return "UNKNOWN";
    }

    private String detectEmploymentType(String markdown) {
        if (markdown == null) return "FULL_TIME";
        String lower = markdown.toLowerCase();
        if (lower.contains("contract") || lower.contains("contractor")) return "CONTRACT";
        if (lower.contains("intern") || lower.contains("internship")) return "INTERNSHIP";
        if (lower.contains("part-time") || lower.contains("part time")) return "PART_TIME";
        return "FULL_TIME";
    }

    private void extractExperience(String markdown, ExtractedJobDetails details) {
        if (markdown == null) return;

        Matcher rangeMatcher = EXPERIENCE_RANGE_PATTERN.matcher(markdown);
        if (rangeMatcher.find()) {
            try {
                details.setMinExperienceYears(new BigDecimal(rangeMatcher.group(1)));
                details.setMaxExperienceYears(new BigDecimal(rangeMatcher.group(2)));
                return;
            } catch (Exception ignored) {}
        }

        Matcher singleMatcher = EXPERIENCE_SINGLE_PATTERN.matcher(markdown);
        if (singleMatcher.find()) {
            try {
                details.setMinExperienceYears(new BigDecimal(singleMatcher.group(1)));
                return;
            } catch (Exception ignored) {}
        }
    }

    private void extractSalary(String markdown, ExtractedJobDetails details) {
        if (markdown == null) return;

        // Check INR LPA (e.g. 15 - 25 LPA)
        Matcher inrLpa = SALARY_INR_LPA_PATTERN.matcher(markdown);
        if (inrLpa.find()) {
            try {
                BigDecimal min = new BigDecimal(inrLpa.group(1)).multiply(BigDecimal.valueOf(100000));
                BigDecimal max = new BigDecimal(inrLpa.group(2)).multiply(BigDecimal.valueOf(100000));
                details.setMinSalary(min);
                details.setMaxSalary(max);
                details.setSalaryCurrency("INR");
                return;
            } catch (Exception ignored) {}
        }

        // Check explicit INR (e.g. ₹15,00,000 - ₹25,00,000)
        Matcher inrExp = SALARY_INR_EXPLICIT_PATTERN.matcher(markdown);
        if (inrExp.find()) {
            try {
                BigDecimal min = new BigDecimal(inrExp.group(1).replace(",", ""));
                BigDecimal max = new BigDecimal(inrExp.group(2).replace(",", ""));
                details.setMinSalary(min);
                details.setMaxSalary(max);
                details.setSalaryCurrency("INR");
                return;
            } catch (Exception ignored) {}
        }

        // Check USD ($120,000 - $160,000)
        Matcher usdMatcher = SALARY_USD_PATTERN.matcher(markdown);
        if (usdMatcher.find()) {
            try {
                BigDecimal min = new BigDecimal(usdMatcher.group(1).replace(",", ""));
                BigDecimal max = new BigDecimal(usdMatcher.group(2).replace(",", ""));
                details.setMinSalary(min);
                details.setMaxSalary(max);
                details.setSalaryCurrency("USD");
                return;
            } catch (Exception ignored) {}
        }
    }

    private LocalDate extractPostingDate(FirecrawlMetadata meta, String markdown) {
        if (meta != null && meta.getDatePosted() != null && !meta.getDatePosted().isBlank()) {
            try {
                String raw = meta.getDatePosted().trim();
                if (raw.length() >= 10) {
                    return LocalDate.parse(raw.substring(0, 10));
                }
            } catch (Exception ignored) {}
        }

        if (markdown != null) {
            Matcher m = DATE_PATTERN.matcher(markdown);
            if (m.find()) {
                try {
                    return LocalDate.parse(m.group(1));
                } catch (Exception ignored) {}
            }
        }

        return null; // Missing data remains null
    }

    private List<String> detectSkills(String markdown) {
        Set<String> matched = new LinkedHashSet<>();
        if (markdown == null || markdown.isBlank()) return new ArrayList<>();

        if (cachedSkills == null) {
            try {
                cachedSkills = skillRepository.findAll();
            } catch (Exception e) {
                cachedSkills = List.of();
            }
        }

        String lowerText = markdown.toLowerCase();

        for (Skill s : cachedSkills) {
            String name = s.getName();
            // Word boundary match
            Pattern p = Pattern.compile("\\b" + Pattern.quote(name.toLowerCase()) + "\\b");
            if (p.matcher(lowerText).find()) {
                matched.add(name);
            }
        }

        // Additional common tech keywords
        List<String> commonTech = List.of("Kubernetes", "AWS", "Kafka", "Redis", "GraphQL", "Microservices", "CI/CD", "Linux");
        for (String tech : commonTech) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(tech.toLowerCase()) + "\\b");
            if (p.matcher(lowerText).find()) {
                matched.add(tech);
            }
        }

        return new ArrayList<>(matched);
    }

    private void extractSections(String markdown, ExtractedJobDetails details) {
        if (markdown == null) return;

        String[] lines = markdown.split("\\r?\\n");
        String currentSection = null;

        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();

            if (lower.startsWith("#") || lower.startsWith("**")) {
                if (lower.contains("responsibilit") || lower.contains("what you'll do")) {
                    currentSection = "RESP";
                    continue;
                } else if (lower.contains("requirement") || lower.contains("qualificat") || lower.contains("who you are")) {
                    currentSection = "REQ";
                    continue;
                } else if (lower.contains("nice to have") || lower.contains("preferred") || lower.contains("bonus")) {
                    currentSection = "PREF";
                    continue;
                }
            }

            if (currentSection != null && (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("^\\d+\\.\\s+.*"))) {
                String bullet = trimmed.replaceFirst("^[\\-*\\d\\.]+\\s*", "").trim();
                if (!bullet.isEmpty()) {
                    if ("RESP".equals(currentSection) && details.getResponsibilities().size() < 10) {
                        details.getResponsibilities().add(bullet);
                    } else if ("REQ".equals(currentSection) && details.getRequiredQualifications().size() < 10) {
                        details.getRequiredQualifications().add(bullet);
                    } else if ("PREF".equals(currentSection) && details.getPreferredQualifications().size() < 10) {
                        details.getPreferredQualifications().add(bullet);
                    }
                }
            }
        }
    }
}
