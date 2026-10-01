package com.jobhunter.service.matching;

import com.jobhunter.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroundingVerificationGateTest {

    private GroundingVerificationGate gate;
    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        gate = new GroundingVerificationGate();

        profile = new CandidateProfile();
        profile.setHeadline("Software Engineer");
        profile.setYearsOfExperience(new BigDecimal("2.5"));

        List<CandidateSkill> skills = new ArrayList<>();
        skills.add(new CandidateSkill(profile, new Skill("Java", "LANGUAGE", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Company", "Commercial backend"));
        skills.add(new CandidateSkill(profile, new Skill("Spring Boot", "FRAMEWORK", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Company", "Production REST APIs"));
        skills.add(new CandidateSkill(profile, new Skill("WebSocket", "CONCEPT", "[]"), "INTERMEDIATE", new BigDecimal("1.0"), false, "PROJECT_ONLY", new BigDecimal("0.80"), "Project", "Mentor project"));
        profile.setSkills(skills);

        List<CandidateExperience> exps = new ArrayList<>();
        exps.add(new CandidateExperience(profile, "FinTech Corp", "Software Engineer", "1.5 yrs", "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]", "[]", "[]", "Finance", "Evidence"));
        profile.setExperiences(exps);
    }

    @Test
    @DisplayName("Should pass grounded claims for verified technologies")
    void testGroundedClaimPasses() {
        GroundingVerificationGate.GroundingResult result =
                gate.verifyTechnicalClaim("Proficiency in Java and Spring Boot", "2.5 years commercial experience in Java and Spring Boot.", profile);

        assertTrue(result.passed());
        assertFalse(result.convertedToGap());
    }

    @Test
    @DisplayName("Should REJECT ungrounded claim and convert to GAP when technology does not exist in profile")
    void testUngroundedClaimRejectedAndConvertedToGap() {
        // Attempt to claim Kafka experience when candidate does NOT have Kafka
        GroundingVerificationGate.GroundingResult result =
                gate.verifyTechnicalClaim("Hands-on experience with Kafka", "Commercial experience with Kafka event streams.", profile);

        assertFalse(result.passed(), "Untruthful Kafka claim must be rejected");
        assertTrue(result.convertedToGap(), "Rejected ungrounded claim must be converted to GAP");
        assertNotNull(result.rejectedReason());
        assertTrue(result.rejectedReason().contains("Kafka"));
    }

    @Test
    @DisplayName("Should demote commercial claim to project-only when technology is only verified in projects")
    void testCommercialDemotionToProjectOnly() {
        // Candidate has WebSocket only as PROJECT_ONLY
        GroundingVerificationGate.GroundingResult result =
                gate.verifyTechnicalClaim("Experience with WebSocket", "Commercial production experience with WebSocket.", profile);

        assertTrue(result.passed());
        assertFalse(result.convertedToGap());
        assertTrue(result.validatedClaim().contains("Project/Academic experience with WebSocket"),
                "Claim must be corrected to project-only experience: " + result.validatedClaim());
    }

    @Test
    @DisplayName("Milestone 6: Microservices technology relationship is grounded when candidate has Spring Boot microservices")
    void testMicroservicesTechnologyRelationshipGrounded() {
        // Candidate has Spring Boot commercial experience
        GroundingVerificationGate.GroundingResult result =
                gate.verifyTechnicalClaim("Hands-on experience with Microservices", "Spring Boot RESTful microservices and API design.", profile);

        assertTrue(result.passed(), "Microservices claim backed by Spring Boot REST microservices must pass grounding");
        assertFalse(result.convertedToGap());
    }
}
