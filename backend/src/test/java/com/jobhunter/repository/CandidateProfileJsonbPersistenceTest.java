package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateExperience;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.CandidateProject;
import com.jobhunter.model.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CandidateProfileJsonbPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateExperienceRepository experienceRepository;

    @Autowired
    private CandidateProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void testCandidateProfileAndRelatedJsonbPersistence() {
        String testEmail = "test_jsonb_" + UUID.randomUUID() + "@jobhunter.ai";
        User user = new User(testEmail, "hash", "Test", "User");
        user = userRepository.save(user);

        // 1. CandidateProfile JSONB fields
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(user);
        profile.setHeadline("Software Engineer");
        profile.setSummary("Test summary");
        profile.setYearsOfExperience(new BigDecimal("3.0"));
        profile.setCurrentLocation("Bangalore");
        profile.setPreferredLocations("[\"Bangalore\", \"Remote\"]");
        profile.setWorkModes("[\"REMOTE\", \"HYBRID\"]");
        profile.setTargetRoles("[\"Backend Engineer\"]");
        profile.setRawProfileData("{\"testKey\": \"testValue\"}");

        CandidateProfile savedProfile = candidateProfileRepository.save(profile);

        // 2. CandidateExperience JSONB fields
        CandidateExperience exp = new CandidateExperience(
                savedProfile,
                "FinTech Solutions",
                "Senior Developer",
                "Jan 2024 - Present",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Led payment gateway\", \"Designed schema\"]",
                "[\"Zero downtime migration\", \"Latency reduced by 40%\"]",
                "FinTech",
                "Engineering"
        );
        CandidateExperience savedExp = experienceRepository.save(exp);

        // 3. CandidateProject JSONB fields
        CandidateProject proj = new CandidateProject(
                savedProfile,
                "Task Queue",
                "Distributed scheduler",
                "[\"Java\", \"Redis\", \"PostgreSQL\"]",
                "Architecture overview",
                "[\"Designed outbox pattern\", \"Wrote tests\"]",
                "[\"10000 executions completed\"]",
                "Source code",
                "https://github.com/test/repo"
        );
        CandidateProject savedProj = projectRepository.save(proj);

        entityManager.flush();
        entityManager.clear();

        // Verify CandidateProfile
        CandidateProfile foundProfile = candidateProfileRepository.findById(savedProfile.getId()).orElseThrow();
        assertEquals("[\"Bangalore\", \"Remote\"]", foundProfile.getPreferredLocations());
        assertEquals("[\"REMOTE\", \"HYBRID\"]", foundProfile.getWorkModes());
        assertEquals("[\"Backend Engineer\"]", foundProfile.getTargetRoles());
        assertEquals("{\"testKey\": \"testValue\"}", foundProfile.getRawProfileData());

        Object prefType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(preferred_locations) FROM candidate_profiles WHERE id = :id")
                .setParameter("id", savedProfile.getId())
                .getSingleResult();
        assertEquals("array", prefType);

        Object workModesType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(work_modes) FROM candidate_profiles WHERE id = :id")
                .setParameter("id", savedProfile.getId())
                .getSingleResult();
        assertEquals("array", workModesType);

        Object targetRolesType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(target_roles) FROM candidate_profiles WHERE id = :id")
                .setParameter("id", savedProfile.getId())
                .getSingleResult();
        assertEquals("array", targetRolesType);

        Object rawType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(raw_profile_data) FROM candidate_profiles WHERE id = :id")
                .setParameter("id", savedProfile.getId())
                .getSingleResult();
        assertEquals("object", rawType);

        // Verify CandidateExperience JSONB fields
        CandidateExperience foundExp = experienceRepository.findById(savedExp.getId()).orElseThrow();
        assertEquals("[\"Java\", \"Spring Boot\", \"PostgreSQL\"]", foundExp.getTechnologies());
        assertEquals("[\"Led payment gateway\", \"Designed schema\"]", foundExp.getResponsibilities());
        assertEquals("[\"Zero downtime migration\", \"Latency reduced by 40%\"]", foundExp.getAchievements());

        Object expTechType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(technologies) FROM candidate_experiences WHERE id = :id")
                .setParameter("id", savedExp.getId())
                .getSingleResult();
        assertEquals("array", expTechType);

        Object expRespType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(responsibilities) FROM candidate_experiences WHERE id = :id")
                .setParameter("id", savedExp.getId())
                .getSingleResult();
        assertEquals("array", expRespType);

        Object expAchType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(achievements) FROM candidate_experiences WHERE id = :id")
                .setParameter("id", savedExp.getId())
                .getSingleResult();
        assertEquals("array", expAchType);

        // Verify CandidateProject JSONB fields
        CandidateProject foundProj = projectRepository.findById(savedProj.getId()).orElseThrow();
        assertEquals("[\"Java\", \"Redis\", \"PostgreSQL\"]", foundProj.getTechnologies());
        assertEquals("[\"Designed outbox pattern\", \"Wrote tests\"]", foundProj.getResponsibilities());
        assertEquals("[\"10000 executions completed\"]", foundProj.getMeasurableOutcomes());

        Object projTechType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(technologies) FROM candidate_projects WHERE id = :id")
                .setParameter("id", savedProj.getId())
                .getSingleResult();
        assertEquals("array", projTechType);

        Object projRespType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(responsibilities) FROM candidate_projects WHERE id = :id")
                .setParameter("id", savedProj.getId())
                .getSingleResult();
        assertEquals("array", projRespType);

        Object projOutcomesType = entityManager.createNativeQuery(
                "SELECT jsonb_typeof(measurable_outcomes) FROM candidate_projects WHERE id = :id")
                .setParameter("id", savedProj.getId())
                .getSingleResult();
        assertEquals("array", projOutcomesType);
    }
}
