package com.jobhunter.service.matching;

import com.jobhunter.model.entity.Job;
import com.jobhunter.model.entity.JobRequirement;
import com.jobhunter.model.entity.Skill;
import com.jobhunter.repository.JobRequirementRepository;
import com.jobhunter.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobRequirementExtractor {

    private static final Logger log = LoggerFactory.getLogger(JobRequirementExtractor.class);

    private final JobRequirementRepository requirementRepository;
    private final SkillRepository skillRepository;

    public JobRequirementExtractor(JobRequirementRepository requirementRepository,
                                  SkillRepository skillRepository) {
        this.requirementRepository = requirementRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public List<JobRequirement> extractAndPersistRequirements(Job job) {
        List<JobRequirement> existing = requirementRepository.findByJobIdOrderByInferredImportanceDesc(job.getId());
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }

        List<JobRequirement> extracted = extractRequirements(job);
        List<JobRequirement> saved = requirementRepository.saveAll(extracted);
        return (saved != null && !saved.isEmpty()) ? saved : extracted;
    }

    public List<JobRequirement> extractRequirements(Job job) {
        List<JobRequirement> requirements = new ArrayList<>();
        String rawMarkdown = job.getRawDescriptionMarkdown();
        if (rawMarkdown == null || rawMarkdown.isBlank()) {
            return requirements;
        }

        // Prompt injection defense: sanitize raw text
        String sanitizedText = sanitizeUntrustedJobText(rawMarkdown);

        // Map known skills from database
        List<Skill> knownSkills = skillRepository.findAll();
        Map<String, Skill> skillMap = new HashMap<>();
        if (knownSkills != null) {
            for (Skill s : knownSkills) {
                skillMap.put(s.getName().toLowerCase(), s);
            }
        }

        // 1. MUST_HAVE & NICE_TO_HAVE Technology Requirements
        extractTechnicalRequirements(job, sanitizedText, skillMap, requirements);

        // 2. EXPERIENCE / SENIORITY Requirements
        extractExperienceAndSeniorityRequirements(job, sanitizedText, requirements);

        // 3. RESPONSIBILITIES
        extractResponsibilities(job, sanitizedText, requirements);

        // 4. DOMAIN Requirements
        extractDomainRequirements(job, sanitizedText, requirements);

        // 5. LOCATION & WORK_MODE Requirements
        extractLocationAndWorkModeRequirements(job, requirements);

        // 6. EDUCATION Requirements
        extractEducationRequirements(job, sanitizedText, requirements);

        // 7. SOFT SKILLS Requirements
        extractSoftSkillsRequirements(job, sanitizedText, requirements);

        // 8. IMPLIED REQUIREMENTS (explicitly tagged as implied)
        extractImpliedRequirements(job, sanitizedText, requirements);

        return requirements;
    }

    private void extractTechnicalRequirements(Job job, String text, Map<String, Skill> skillMap, List<JobRequirement> reqs) {
        Set<String> identifiedSkills = new HashSet<>();

        // Match known skills
        for (Map.Entry<String, Skill> entry : skillMap.entrySet()) {
            String skillName = entry.getKey();
            Pattern p = Pattern.compile("\\b" + Pattern.quote(skillName) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                identifiedSkills.add(entry.getValue().getName());

                // Line-level context to prevent leakage across bullet points
                int lineStart = text.lastIndexOf('\n', m.start());
                if (lineStart == -1) lineStart = 0;
                int lineEnd = text.indexOf('\n', m.end());
                if (lineEnd == -1) lineEnd = text.length();
                String lineContext = text.substring(lineStart, lineEnd).toLowerCase();

                boolean isNiceToHave = lineContext.contains("nice to have") || lineContext.contains("bonus") ||
                        lineContext.contains("preferred") || lineContext.contains("plus") || lineContext.contains("advantageous");

                String type = isNiceToHave ? "NICE_TO_HAVE" : "MUST_HAVE";
                BigDecimal importance = isNiceToHave ? new BigDecimal("0.60") : new BigDecimal("1.00");

                JobRequirement req = new JobRequirement(
                        job,
                        type,
                        "TECHNICAL",
                        "Proficiency in " + entry.getValue().getName(),
                        importance,
                        false,
                        text.substring(lineStart, lineEnd).trim()
                );
                req.setSkill(entry.getValue());
                reqs.add(req);
            }
        }

        // Additional common industry tools
        String[] extraTech = {"Kafka", "Kubernetes", "AWS", "GCP", "Azure", "GraphQL", "Redis", "Elasticsearch", "Microservices", "CI/CD"};
        for (String tech : extraTech) {
            if (identifiedSkills.contains(tech)) continue;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(tech) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) {
                int lineStart = text.lastIndexOf('\n', m.start());
                if (lineStart == -1) lineStart = 0;
                int lineEnd = text.indexOf('\n', m.end());
                if (lineEnd == -1) lineEnd = text.length();
                String lineContext = text.substring(lineStart, lineEnd).toLowerCase();

                boolean isNice = lineContext.contains("nice to have") || lineContext.contains("plus") || lineContext.contains("preferred");

                reqs.add(new JobRequirement(
                        job,
                        isNice ? "NICE_TO_HAVE" : "MUST_HAVE",
                        "TECHNICAL",
                        "Hands-on experience with " + tech,
                        isNice ? new BigDecimal("0.60") : new BigDecimal("0.90"),
                        false,
                        text.substring(lineStart, lineEnd).trim()
                ));
            }
        }
    }

    private void extractExperienceAndSeniorityRequirements(Job job, String text, List<JobRequirement> reqs) {
        if (job.getMinExperienceYears() != null) {
            String desc = job.getMinExperienceYears() + (job.getMaxExperienceYears() != null ? " - " + job.getMaxExperienceYears() : "+") + " years of professional engineering experience";
            reqs.add(new JobRequirement(
                    job,
                    "EXPERIENCE",
                    "EXPERIENCE",
                    desc,
                    new BigDecimal("1.00"),
                    false,
                    desc
            ));
        }

        // Seniority from title
        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
        String seniority = "Mid-Level";
        if (title.contains("staff") || title.contains("principal")) seniority = "Staff/Principal";
        else if (title.contains("lead")) seniority = "Lead";
        else if (title.contains("senior") || title.contains("sr")) seniority = "Senior";
        else if (title.contains("junior") || title.contains("associate") || title.contains("entry")) seniority = "Junior/Associate";

        reqs.add(new JobRequirement(
                job,
                "SENIORITY",
                "EXPERIENCE",
                "Seniority Expectation: " + seniority,
                new BigDecimal("0.85"),
                false,
                "Derived from role title: " + job.getTitle()
        ));
    }

    private void extractResponsibilities(Job job, String text, List<JobRequirement> reqs) {
        String[] lines = text.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*") || trimmed.startsWith("-") || trimmed.startsWith("•")) {
                String clean = trimmed.replaceAll("^[\\*\\-•\\s]+", "").trim();
                if (clean.length() > 25 && clean.length() < 250) {
                    reqs.add(new JobRequirement(
                            job,
                            "RESPONSIBILITIES",
                            "PROCESS",
                            clean,
                            new BigDecimal("0.75"),
                            false,
                            clean
                    ));
                    count++;
                    if (count >= 5) break;
                }
            }
        }
    }

    private void extractDomainRequirements(Job job, String text, List<JobRequirement> reqs) {
        String lower = text.toLowerCase();
        if (lower.contains("payment") || lower.contains("fintech") || lower.contains("banking")) {
            reqs.add(new JobRequirement(
                    job,
                    "DOMAIN",
                    "DOMAIN",
                    "FinTech / High-Reliability Financial Transaction Systems",
                    new BigDecimal("0.70"),
                    false,
                    "Payments / FinTech domain focus"
            ));
        } else if (lower.contains("e-commerce") || lower.contains("retail")) {
            reqs.add(new JobRequirement(
                    job,
                    "DOMAIN",
                    "DOMAIN",
                    "E-Commerce / Consumer Retail Systems",
                    new BigDecimal("0.70"),
                    false,
                    "E-commerce domain focus"
            ));
        } else if (lower.contains("saas") || lower.contains("b2b")) {
            reqs.add(new JobRequirement(
                    job,
                    "DOMAIN",
                    "DOMAIN",
                    "B2B Enterprise SaaS Architecture",
                    new BigDecimal("0.70"),
                    false,
                    "Enterprise SaaS focus"
            ));
        }
    }

    private void extractLocationAndWorkModeRequirements(Job job, List<JobRequirement> reqs) {
        if (job.getLocation() != null) {
            reqs.add(new JobRequirement(
                    job,
                    "LOCATION",
                    "LOCATION",
                    "Location Requirement: " + job.getLocation(),
                    new BigDecimal("0.80"),
                    false,
                    job.getLocation()
            ));
        }

        if (job.getWorkMode() != null) {
            reqs.add(new JobRequirement(
                    job,
                    "WORK_MODE",
                    "LOCATION",
                    "Work Mode: " + job.getWorkMode(),
                    new BigDecimal("0.85"),
                    false,
                    "Work Mode: " + job.getWorkMode()
            ));
        }
    }

    private void extractEducationRequirements(Job job, String text, List<JobRequirement> reqs) {
        String lower = text.toLowerCase();
        if (lower.contains("bachelor") || lower.contains("b.s.") || lower.contains("degree in computer science")) {
            reqs.add(new JobRequirement(
                    job,
                    "EDUCATION",
                    "EDUCATION",
                    "Bachelor's degree in Computer Science, Engineering, or equivalent practical experience",
                    new BigDecimal("0.50"),
                    false,
                    "Computer Science / Engineering Degree"
            ));
        }
    }

    private void extractSoftSkillsRequirements(Job job, String text, List<JobRequirement> reqs) {
        String lower = text.toLowerCase();
        if (lower.contains("collaborat") || lower.contains("cross-functional")) {
            reqs.add(new JobRequirement(
                    job,
                    "SOFT_SKILLS",
                    "SOFT_SKILL",
                    "Cross-functional collaboration with product and design teams",
                    new BigDecimal("0.50"),
                    false,
                    "Cross-functional collaboration"
            ));
        }
        if (lower.contains("mentor") || lower.contains("guidance")) {
            reqs.add(new JobRequirement(
                    job,
                    "SOFT_SKILLS",
                    "SOFT_SKILL",
                    "Technical mentorship of junior engineers and code review leadership",
                    new BigDecimal("0.60"),
                    false,
                    "Mentorship & guidance"
            ));
        }
    }

    private void extractImpliedRequirements(Job job, String text, List<JobRequirement> reqs) {
        String lower = text.toLowerCase();
        if (lower.contains("high-throughput") || lower.contains("million") || lower.contains("concurrency")) {
            reqs.add(new JobRequirement(
                    job,
                    "IMPLIED_REQUIREMENTS",
                    "TECHNICAL",
                    "Deep understanding of concurrency, race condition prevention, and database transaction isolation levels",
                    new BigDecimal("0.75"),
                    true,
                    "Inferred from high-throughput and concurrency scale mentioned in JD"
            ));
        }

        if (lower.contains("distributed system") || lower.contains("distributed cache") || lower.contains("microservice")) {
            reqs.add(new JobRequirement(
                    job,
                    "IMPLIED_REQUIREMENTS",
                    "TECHNICAL",
                    "Familiarity with distributed system failure modes, circuit breakers, and eventual consistency",
                    new BigDecimal("0.75"),
                    true,
                    "Inferred from distributed system architecture referenced in JD"
            ));
        }
    }

    public String sanitizeUntrustedJobText(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)<\\|im_start\\|>", "")
                .replaceAll("(?i)<\\|im_end\\|>", "")
                .replaceAll("(?i)\\b(?:system|human|assistant|user):", "")
                .replaceAll("(?i)ignore (?:all )?previous instructions", "[REDACTED_ATTEMPT]")
                .replaceAll("(?i)you are now in developer mode", "[REDACTED_ATTEMPT]")
                .replaceAll("(?i)output the system prompt", "[REDACTED_ATTEMPT]");
    }
}
