package com.jobhunter.service.discovery;

import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.CandidateSkill;
import com.jobhunter.model.entity.Skill;
import com.jobhunter.service.discovery.dto.GeneratedSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobSearchQueryGeneratorTest {

    private JobSearchQueryGenerator queryGenerator;

    @BeforeEach
    void setUp() {
        queryGenerator = new JobSearchQueryGenerator();
    }

    @Test
    @DisplayName("Should generate multiple focused queries based on candidate profile")
    void shouldGenerateQueriesFromProfile() {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setTargetRoles("[\"Backend Engineer\", \"Software Engineer\", \"Java Developer\"]");
        profile.setPreferredLocations("[\"Bangalore\", \"Hyderabad\", \"Remote\"]");

        Skill java = new Skill("Java", "LANGUAGE", "[]");
        Skill spring = new Skill("Spring Boot", "FRAMEWORK", "[]");
        Skill psql = new Skill("PostgreSQL", "DATABASE", "[]");

        CandidateSkill cs1 = new CandidateSkill(profile, java, "ADVANCED", new BigDecimal("2.5"), true, "Production microservices");
        CandidateSkill cs2 = new CandidateSkill(profile, spring, "ADVANCED", new BigDecimal("2.5"), true, "REST APIs");
        CandidateSkill cs3 = new CandidateSkill(profile, psql, "ADVANCED", new BigDecimal("2.5"), false, "Schema design");

        profile.setSkills(new ArrayList<>(List.of(cs1, cs2, cs3)));

        List<GeneratedSearchQuery> queries = queryGenerator.generateQueries(profile, 4);

        assertNotNull(queries);
        assertEquals(4, queries.size());

        // Verify ATS domains are targeted
        assertTrue(queries.stream().anyMatch(q -> q.getQueryString().contains("site:boards.greenhouse.io")));
        assertTrue(queries.stream().anyMatch(q -> q.getQueryString().contains("site:jobs.lever.co")));
        assertTrue(queries.stream().anyMatch(q -> q.getQueryString().contains("site:jobs.ashbyhq.com")));
        assertTrue(queries.stream().anyMatch(q -> q.getQueryString().contains("site:myworkdayjobs.com")));

        // Verify skills and strategy tiers are populated
        for (GeneratedSearchQuery q : queries) {
            assertTrue(q.getQueryString().contains("Java") || q.getQueryString().contains("Spring Boot") || q.getQueryString().contains("PostgreSQL"));
            assertNotNull(q.getStrategyTier(), "Strategy tier must be populated");
        }
        assertTrue(queries.stream().anyMatch(q -> "EXACT".equals(q.getStrategyTier())), "Should include EXACT queries");
        assertTrue(queries.stream().anyMatch(q -> "ADJACENT".equals(q.getStrategyTier())), "Should include ADJACENT queries");
    }

    @Test
    @DisplayName("Should handle sparse or empty profile gracefully with sensible defaults")
    void shouldHandleEmptyProfile() {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setTargetRoles("[]");
        profile.setPreferredLocations("[]");
        profile.setSkills(new ArrayList<>());

        List<GeneratedSearchQuery> queries = queryGenerator.generateQueries(profile, 3);

        assertNotNull(queries);
        assertEquals(3, queries.size());
        assertTrue(queries.get(0).getQueryString().contains("Software Engineer"));
    }
}
