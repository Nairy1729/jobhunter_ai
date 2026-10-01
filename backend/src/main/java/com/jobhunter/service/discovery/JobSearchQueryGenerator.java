package com.jobhunter.service.discovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.CandidateSkill;
import com.jobhunter.service.discovery.dto.GeneratedSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class JobSearchQueryGenerator {

    private static final Logger log = LoggerFactory.getLogger(JobSearchQueryGenerator.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> ATS_DOMAINS = List.of(
            "site:boards.greenhouse.io",
            "site:jobs.lever.co",
            "site:jobs.ashbyhq.com",
            "site:myworkdayjobs.com"
    );

    public List<GeneratedSearchQuery> generateQueries(CandidateProfile profile, int maxQueries) {
        List<GeneratedSearchQuery> queries = new ArrayList<>();

        // 1. Extract Target Roles
        List<String> targetRoles = parseJsonList(profile.getTargetRoles());
        if (targetRoles.isEmpty()) {
            targetRoles = List.of("Software Engineer", "Backend Engineer");
        }

        // 2. Extract Skills (Primary first, then secondary)
        List<String> primarySkills = new ArrayList<>();
        List<String> secondarySkills = new ArrayList<>();

        if (profile.getSkills() != null) {
            for (CandidateSkill cs : profile.getSkills()) {
                if (cs.getSkill() != null && cs.getSkill().getName() != null) {
                    if (cs.isPrimary()) {
                        primarySkills.add(cs.getSkill().getName());
                    } else {
                        secondarySkills.add(cs.getSkill().getName());
                    }
                }
            }
        }

        if (primarySkills.isEmpty()) {
            primarySkills.addAll(List.of("Java", "Spring Boot", "PostgreSQL"));
        }

        // 3. Extract Preferred Locations
        List<String> locations = parseJsonList(profile.getPreferredLocations());
        // Clean location names (remove "International" or generic words if not helpful in query)
        List<String> queryLocations = locations.stream()
                .filter(l -> !l.equalsIgnoreCase("International") && !l.isBlank())
                .collect(Collectors.toList());

        // Build 2 skill clusters
        List<String> coreSkillsCluster1 = primarySkills.stream().limit(2).toList();
        List<String> coreSkillsCluster2 = primarySkills.stream().skip(2).limit(2).toList();
        if (coreSkillsCluster2.isEmpty()) {
            coreSkillsCluster2 = secondarySkills.stream().limit(2).toList();
        }
        if (coreSkillsCluster2.isEmpty()) {
            coreSkillsCluster2 = coreSkillsCluster1;
        }

        // Build role clusters (up to 2 roles per query to keep search focused)
        List<String> roleCluster1 = targetRoles.stream().limit(2).toList();
        List<String> roleCluster2 = targetRoles.stream().skip(2).limit(2).toList();
        if (roleCluster2.isEmpty()) {
            roleCluster2 = roleCluster1;
        }

        // Build adjacent roles from target roles
        List<String> adjacentRoles = deriveAdjacentRoles(targetRoles);
        List<String> adjacentRoleCluster = adjacentRoles.stream().limit(2).toList();
        if (adjacentRoleCluster.isEmpty()) {
            adjacentRoleCluster = roleCluster1;
        }

        // Location clause
        String locationClause = "";
        if (!queryLocations.isEmpty()) {
            List<String> topLocations = queryLocations.stream().limit(2).toList();
            locationClause = " (" + topLocations.stream().map(l -> "\"" + l + "\"").collect(Collectors.joining(" OR ")) + ")";
        }

        Set<String> seenQueries = new HashSet<>();

        // --- STRATEGY TIER 1: EXACT (Target Roles + Core Skills) ---
        addIfUnique(queries, seenQueries, buildQuery(
                "site:boards.greenhouse.io",
                "GREENHOUSE",
                coreSkillsCluster1,
                roleCluster1,
                queryLocations,
                locationClause,
                "EXACT"
        ));

        addIfUnique(queries, seenQueries, buildQuery(
                "site:jobs.lever.co",
                "LEVER",
                coreSkillsCluster1,
                roleCluster2,
                queryLocations,
                locationClause,
                "EXACT"
        ));

        // --- STRATEGY TIER 2: ADJACENT (Adjacent Titles + Core Skills) ---
        addIfUnique(queries, seenQueries, buildQuery(
                "site:jobs.ashbyhq.com",
                "ASHBY",
                coreSkillsCluster2,
                adjacentRoleCluster,
                queryLocations,
                "", // Ashby is often global/remote-first, test without strict location
                "ADJACENT"
        ));

        addIfUnique(queries, seenQueries, buildQuery(
                "site:myworkdayjobs.com",
                "WORKDAY",
                coreSkillsCluster1,
                adjacentRoleCluster,
                queryLocations,
                locationClause,
                "ADJACENT"
        ));

        // --- STRATEGY TIER 3: SKILL-LED (Core Stack Combinations) ---
        List<String> highSignalStack = new ArrayList<>(coreSkillsCluster1);
        if (!coreSkillsCluster2.isEmpty() && !coreSkillsCluster2.equals(coreSkillsCluster1)) {
            highSignalStack.addAll(coreSkillsCluster2.stream().limit(1).toList());
        }
        addIfUnique(queries, seenQueries, buildSkillLedQuery(
                "site:boards.greenhouse.io",
                "GREENHOUSE",
                highSignalStack,
                queryLocations,
                locationClause
        ));

        addIfUnique(queries, seenQueries, buildSkillLedQuery(
                "site:jobs.lever.co",
                "LEVER",
                highSignalStack,
                queryLocations,
                ""
        ));

        if (maxQueries > 6) {
            addIfUnique(queries, seenQueries, buildQuery(
                    "site:jobs.ashbyhq.com",
                    "ASHBY",
                    coreSkillsCluster1,
                    roleCluster1,
                    queryLocations,
                    locationClause,
                    "EXACT"
            ));
        }

        int limit = Math.min(queries.size(), Math.max(1, maxQueries));
        List<GeneratedSearchQuery> result = queries.subList(0, limit);
        log.info("QUERY_GENERATOR - Generated [{}] search queries for candidate profile [{}]", result.size(), profile.getId());
        return result;
    }

    private void addIfUnique(List<GeneratedSearchQuery> queries, Set<String> seen, GeneratedSearchQuery query) {
        String normalized = query.getQueryString().trim().toLowerCase();
        if (seen.add(normalized)) {
            queries.add(query);
        }
    }

    private List<String> deriveAdjacentRoles(List<String> targetRoles) {
        Set<String> adjacent = new LinkedHashSet<>();
        for (String r : targetRoles) {
            String lower = r.toLowerCase();
            if (lower.contains("backend")) {
                adjacent.add("Platform Engineer");
                adjacent.add("Systems Engineer");
                adjacent.add("Java Developer");
            } else if (lower.contains("frontend")) {
                adjacent.add("UI Engineer");
                adjacent.add("Web Developer");
            } else if (lower.contains("full stack") || lower.contains("fullstack")) {
                adjacent.add("Software Developer");
                adjacent.add("Product Engineer");
            } else if (lower.contains("devops")) {
                adjacent.add("Site Reliability Engineer");
                adjacent.add("Infrastructure Engineer");
            } else {
                adjacent.add("Backend Engineer");
                adjacent.add("Systems Engineer");
            }
        }
        return new ArrayList<>(adjacent);
    }

    private GeneratedSearchQuery buildQuery(
            String siteDomain,
            String sourceIdentifier,
            List<String> skills,
            List<String> roles,
            List<String> locations,
            String locationClause) {
        return buildQuery(siteDomain, sourceIdentifier, skills, roles, locations, locationClause, "EXACT");
    }

    private GeneratedSearchQuery buildQuery(
            String siteDomain,
            String sourceIdentifier,
            List<String> skills,
            List<String> roles,
            List<String> locations,
            String locationClause,
            String strategyTier) {

        String skillsClause = "(" + skills.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(" OR ")) + ")";
        String rolesClause = "(" + roles.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(" OR ")) + ")";

        String queryString = siteDomain + " " + skillsClause + " " + rolesClause + locationClause;

        return new GeneratedSearchQuery(queryString, sourceIdentifier, roles, skills, locations, strategyTier);
    }

    private GeneratedSearchQuery buildSkillLedQuery(
            String siteDomain,
            String sourceIdentifier,
            List<String> skills,
            List<String> locations,
            String locationClause) {

        String skillsClause = "(" + skills.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(" AND ")) + ")";
        String genericRoles = "(\"Engineer\" OR \"Developer\")";
        String queryString = siteDomain + " " + skillsClause + " " + genericRoles + locationClause;

        return new GeneratedSearchQuery(queryString, sourceIdentifier, List.of("Engineer", "Developer"), skills, locations, "SKILL_LED");
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Could not parse JSON list: {}", json);
            return Collections.emptyList();
        }
    }
}
