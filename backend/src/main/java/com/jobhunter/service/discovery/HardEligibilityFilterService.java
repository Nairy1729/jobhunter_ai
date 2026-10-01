package com.jobhunter.service.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Job;
import com.jobhunter.service.discovery.dto.HardEligibilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hard Eligibility Filter.
 * Eliminates obvious mismatches before expensive semantic matching.
 * Disqualifies only structural incompatibilities:
 * - Severe experience disparity (e.g. 2 yrs vs 8+ yrs)
 * - Obvious role domain mismatch (e.g. Software Engineer vs Product Designer/Nurse)
 * - Incompatible location/work authorization without relocation/remote allowance
 * - Unmet hard education gates (e.g. PhD mandatory)
 *
 * Invariant: Never rejects a job simply because an optional skill or minor preference is missing.
 */
@Service
public class HardEligibilityFilterService {

    private static final Logger log = LoggerFactory.getLogger(HardEligibilityFilterService.class);

    private final ObjectMapper objectMapper;

    // Obvious non-technical / mismatched professions when candidate targets software / tech
    private static final List<String> NON_ENG_ROLES = List.of(
            "product designer", "ui/ux designer", "graphic designer", "visual designer",
            "sales representative", "account executive", "sales development", "bdr",
            "registered nurse", "nurse practitioner", "physician", "medical assistant",
            "legal counsel", "attorney", "paralegal", "barista", "culinary",
            "recruiter", "talent acquisition specialist", "customer support agent"
    );

    private static final Pattern SENIORITY_YEARS_PATTERN = Pattern.compile("(?i)\\b(\\d{1,2})\\+?\\s*years?\\b");
    private static final Pattern PHD_MANDATORY_PATTERN = Pattern.compile("(?i)\\b(ph\\.?d\\.?|doctorate)\\s+(mandatory|required|must have)\\b");

    public HardEligibilityFilterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HardEligibilityResult evaluate(Job job, CandidateProfile profile) {
        if (job == null || profile == null) {
            return HardEligibilityResult.pass();
        }

        String title = job.getTitle() != null ? job.getTitle().toLowerCase().trim() : "";
        String desc = job.getRawDescriptionMarkdown() != null ? job.getRawDescriptionMarkdown().toLowerCase() : "";
        double candYoe = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().doubleValue() : 0.0;

        // 1. Role Domain Check
        List<String> targetRoles = parseJsonList(profile.getTargetRoles());
        boolean candidateIsTech = targetRoles.stream().anyMatch(r -> {
            String lower = r.toLowerCase();
            return lower.contains("engineer") || lower.contains("developer") || lower.contains("software") || lower.contains("backend") || lower.contains("frontend") || lower.contains("full stack") || lower.contains("tech");
        }) || (profile.getHeadline() != null && profile.getHeadline().toLowerCase().contains("engineer"));

        if (candidateIsTech) {
            for (String nonEng : NON_ENG_ROLES) {
                if (title.contains(nonEng)) {
                    log.info("HARD_FILTER_REJECT - Role domain disconnect: Job [{}] is [{}] vs tech candidate", job.getId(), nonEng);
                    return HardEligibilityResult.reject("Role domain mismatch: Job targets " + nonEng + ", which is outside candidate's engineering domain.", "ROLE_DOMAIN_MISMATCH");
                }
            }
        }

        // 2. Severe Experience Desynchronization Check
        if (job.getMinExperienceYears() != null && job.getMinExperienceYears().doubleValue() > candYoe + 4.0) {
            double reqYoe = job.getMinExperienceYears().doubleValue();
            log.info("HARD_FILTER_REJECT - Severe experience delta: Job [{}] requires {} yrs vs candidate {} yrs", job.getId(), reqYoe, candYoe);
            return HardEligibilityResult.reject("Severe experience delta: Role mandates " + reqYoe + "+ years (candidate has " + candYoe + " years).", "SENIORITY_OVERFLOW");
        }

        if (candYoe < 5.0) {
            if (title.contains("staff engineer") || title.contains("principal engineer") || title.contains("engineering manager") || title.contains("vp of") || title.contains("director of engineering")) {
                log.info("HARD_FILTER_REJECT - Executive/Staff role delta: Job [{}] title [{}] vs cand YoE {}", job.getId(), title, candYoe);
                return HardEligibilityResult.reject("Role targets Staff/Principal/Director level (" + title + "), which exceeds candidate's current commercial tenure of " + candYoe + " years.", "SENIORITY_OVERFLOW");
            }
        }

        Matcher ym = SENIORITY_YEARS_PATTERN.matcher(title);
        if (ym.find()) {
            try {
                int reqY = Integer.parseInt(ym.group(1));
                if (reqY >= 8 && candYoe < 4.0) {
                    return HardEligibilityResult.reject("Role specifies " + reqY + "+ years in title (candidate has " + candYoe + " years).", "SENIORITY_OVERFLOW");
                }
            } catch (Exception ignored) {}
        }

        // 3. Location / Work Authorization Incompatibility
        String jobLoc = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
        String candLoc = profile.getCurrentLocation() != null ? profile.getCurrentLocation().toLowerCase() : "";
        List<String> prefLocs = parseJsonList(profile.getPreferredLocations());
        List<String> workModes = parseJsonList(profile.getWorkModes());

        boolean candPermitsRemote = workModes.stream().anyMatch(w -> w.equalsIgnoreCase("REMOTE")) || prefLocs.stream().anyMatch(p -> p.toLowerCase().contains("remote"));
        boolean candPermitsRelocation = workModes.stream().anyMatch(w -> w.equalsIgnoreCase("RELOCATION"));
        boolean jobIsRemote = "REMOTE".equalsIgnoreCase(job.getWorkMode()) || jobLoc.contains("remote");

        boolean foreignOnSite = (jobLoc.contains("united states") || jobLoc.contains("usa") || jobLoc.contains("new york") || jobLoc.contains("san francisco") || jobLoc.contains("london") || jobLoc.contains("uk") || jobLoc.contains("singapore"))
                && !candLoc.contains("united states") && !candLoc.contains("usa") && !candLoc.contains("uk");

        if (foreignOnSite && !jobIsRemote && !candPermitsRelocation) {
            // Strictly on-site abroad
            if (desc.contains("must be authorized to work in the u") || desc.contains("us citizenship required") || desc.contains("no sponsorship") || desc.contains("in-office full time") || desc.contains("on-site in")) {
                log.info("HARD_FILTER_REJECT - Incompatible foreign location without relocation allowance: Job [{}]", job.getId());
                return HardEligibilityResult.reject("On-site foreign role (" + job.getLocation() + ") requires local work authorization without remote allowance.", "LOCATION_INCOMPATIBLE");
            }
        }

        // 4. Hard Education Gates (PhD Mandatory)
        if (PHD_MANDATORY_PATTERN.matcher(desc).find()) {
            boolean candHasPhd = (profile.getSummary() != null && profile.getSummary().toLowerCase().contains("phd"))
                    || (profile.getRawProfileData() != null && profile.getRawProfileData().toLowerCase().contains("phd"));
            if (!candHasPhd) {
                log.info("HARD_FILTER_REJECT - PhD mandatory gate unmet for Job [{}]", job.getId());
                return HardEligibilityResult.reject("Job mandates a PhD or Doctorate which is not verified in candidate evidence.", "EDUCATION_UNMET");
            }
        }

        return HardEligibilityResult.pass();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            String clean = json.replaceAll("[\\[\\]\"']", "").trim();
            if (clean.isBlank()) return Collections.emptyList();
            return Arrays.stream(clean.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
    }
}
