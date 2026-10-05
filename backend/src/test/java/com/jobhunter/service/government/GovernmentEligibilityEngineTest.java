package com.jobhunter.service.government;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.government.EligibilityEvaluationResultDto;
import com.jobhunter.model.entity.government.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GovernmentEligibilityEngineTest {

    private GovernmentEligibilityEngine engine;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        engine = new GovernmentEligibilityEngine(objectMapper);
    }

    @Test
    @DisplayName("Candidate satisfies age, education, and female-only criteria -> ELIGIBLE")
    void testEligibleCandidate() throws Exception {
        CandidateGovernmentProfile profile = new CandidateGovernmentProfile();
        profile.setAge(24);
        profile.setGender("FEMALE");
        profile.setHighestEducation("Graduate");
        profile.setDegreesJson(objectMapper.writeValueAsString(List.of("B.Tech", "12th Pass")));
        profile.setState("Uttar Pradesh");
        profile.setDistrict("Kannauj");
        profile.setDomicileState("Uttar Pradesh");
        profile.setDomicileDistrict("Kannauj");

        GovernmentJob job = new GovernmentJob();
        job.setTitle("Anganwadi Worker");
        GovernmentJobEligibility eligibility = new GovernmentJobEligibility();
        eligibility.setJob(job);
        eligibility.setMinimumAge(18);
        eligibility.setMaximumAge(35);
        eligibility.setGender(GenderEligibility.FEMALE_ONLY);
        eligibility.setEducationJson(objectMapper.writeValueAsString(List.of("12th Pass")));
        eligibility.setDomicile("Resident of Kannauj District, Uttar Pradesh");
        job.setEligibility(eligibility);

        EligibilityEvaluationResultDto result = engine.evaluate(profile, job);

        assertEquals(EligibilityStatus.ELIGIBLE, result.getStatus());
        assertTrue(result.getReasons().stream().allMatch(r -> "PASS".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Male candidate applying for female-only post is strictly NOT_ELIGIBLE")
    void testFemaleOnlyRejection() throws Exception {
        CandidateGovernmentProfile profile = new CandidateGovernmentProfile();
        profile.setAge(24);
        profile.setGender("MALE");
        profile.setHighestEducation("Graduate");

        GovernmentJob job = new GovernmentJob();
        GovernmentJobEligibility eligibility = new GovernmentJobEligibility();
        eligibility.setJob(job);
        eligibility.setGender(GenderEligibility.FEMALE_ONLY);
        job.setEligibility(eligibility);

        EligibilityEvaluationResultDto result = engine.evaluate(profile, job);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
        assertTrue(result.getReasons().stream().anyMatch(r -> "GENDER".equals(r.getCriterion()) && "FAIL".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Candidate exceeding maximum age is strictly NOT_ELIGIBLE")
    void testOverAgeRejection() {
        CandidateGovernmentProfile profile = new CandidateGovernmentProfile();
        profile.setAge(39);
        profile.setGender("FEMALE");
        profile.setHighestEducation("Graduate");

        GovernmentJob job = new GovernmentJob();
        GovernmentJobEligibility eligibility = new GovernmentJobEligibility();
        eligibility.setJob(job);
        eligibility.setMinimumAge(18);
        eligibility.setMaximumAge(35);
        eligibility.setGender(GenderEligibility.ALL);
        job.setEligibility(eligibility);

        EligibilityEvaluationResultDto result = engine.evaluate(profile, job);

        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
        assertTrue(result.getReasons().stream().anyMatch(r -> "AGE".equals(r.getCriterion()) && "FAIL".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Incomplete candidate profile returns UNKNOWN or LIKELY_ELIGIBLE without incorrect rejection")
    void testIncompleteProfileHandling() throws Exception {
        CandidateGovernmentProfile profile = new CandidateGovernmentProfile();
        profile.setAge(24);
        // Missing education, gender, domicile

        GovernmentJob job = new GovernmentJob();
        GovernmentJobEligibility eligibility = new GovernmentJobEligibility();
        eligibility.setJob(job);
        eligibility.setMinimumAge(18);
        eligibility.setMaximumAge(35);
        eligibility.setGender(GenderEligibility.FEMALE_ONLY);
        eligibility.setEducationJson(objectMapper.writeValueAsString(List.of("12th Pass")));
        job.setEligibility(eligibility);

        EligibilityEvaluationResultDto result = engine.evaluate(profile, job);

        assertNotEquals(EligibilityStatus.NOT_ELIGIBLE, result.getStatus());
        assertFalse(result.getMissingProfileFields().isEmpty());
    }
}
