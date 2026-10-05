package com.jobhunter.service.government;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.government.EligibilityEvaluationResultDto;
import com.jobhunter.model.dto.government.EligibilityReasonDto;
import com.jobhunter.model.entity.government.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GovernmentEligibilityEngine {

    private final ObjectMapper objectMapper;

    // Academic hierarchy score for standard qualification checks
    private static final Map<String, Integer> EDUCATION_LEVELS = new HashMap<>();
    static {
        EDUCATION_LEVELS.put("8TH", 1);
        EDUCATION_LEVELS.put("8TH PASS", 1);
        EDUCATION_LEVELS.put("10TH", 2);
        EDUCATION_LEVELS.put("10TH PASS", 2);
        EDUCATION_LEVELS.put("MATRICULATION", 2);
        EDUCATION_LEVELS.put("SSC", 2);
        EDUCATION_LEVELS.put("12TH", 3);
        EDUCATION_LEVELS.put("12TH PASS", 3);
        EDUCATION_LEVELS.put("INTERMEDIATE", 3);
        EDUCATION_LEVELS.put("HSC", 3);
        EDUCATION_LEVELS.put("ITI", 3);
        EDUCATION_LEVELS.put("DIPLOMA", 4);
        EDUCATION_LEVELS.put("POLYTECHNIC", 4);
        EDUCATION_LEVELS.put("GRADUATE", 5);
        EDUCATION_LEVELS.put("GRADUATION", 5);
        EDUCATION_LEVELS.put("DEGREE", 5);
        EDUCATION_LEVELS.put("BACHELORS", 5);
        EDUCATION_LEVELS.put("B.A", 5);
        EDUCATION_LEVELS.put("B.SC", 5);
        EDUCATION_LEVELS.put("B.COM", 5);
        EDUCATION_LEVELS.put("B.TECH", 5);
        EDUCATION_LEVELS.put("B.E", 5);
        EDUCATION_LEVELS.put("POST GRADUATE", 6);
        EDUCATION_LEVELS.put("POSTGRADUATE", 6);
        EDUCATION_LEVELS.put("MASTERS", 6);
        EDUCATION_LEVELS.put("M.TECH", 6);
        EDUCATION_LEVELS.put("M.E", 6);
        EDUCATION_LEVELS.put("M.SC", 6);
        EDUCATION_LEVELS.put("MBA", 6);
        EDUCATION_LEVELS.put("PHD", 7);
        EDUCATION_LEVELS.put("DOCTORATE", 7);
    }

    public GovernmentEligibilityEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EligibilityEvaluationResultDto evaluate(CandidateGovernmentProfile profile, GovernmentJob job) {
        if (job == null) {
            return new EligibilityEvaluationResultDto(EligibilityStatus.UNKNOWN, "Job data unavailable", Collections.emptyList(), Collections.emptyList());
        }

        GovernmentJobEligibility eligibility = job.getEligibility();
        if (eligibility == null) {
            return new EligibilityEvaluationResultDto(
                    EligibilityStatus.LIKELY_ELIGIBLE,
                    "Eligibility criteria not strictly restricted in notification",
                    List.of(new EligibilityReasonDto("GENERAL", "PASS", "No restrictive eligibility constraints extracted")),
                    Collections.emptyList()
            );
        }

        if (profile == null) {
            return new EligibilityEvaluationResultDto(
                    EligibilityStatus.UNKNOWN,
                    "Candidate government profile not filled. Complete profile to verify eligibility.",
                    List.of(new EligibilityReasonDto("PROFILE", "UNKNOWN", "Profile details missing")),
                    List.of("Age", "Gender", "State", "District", "Education")
            );
        }

        List<EligibilityReasonDto> reasons = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        boolean hasFail = false;
        boolean hasUnknown = false;

        // 1. AGE MATCHING
        Integer minAge = eligibility.getMinimumAge();
        Integer maxAge = eligibility.getMaximumAge();
        Integer candAge = profile.getAge();

        if (minAge != null || maxAge != null) {
            if (candAge == null) {
                hasUnknown = true;
                missingFields.add("Age");
                reasons.add(new EligibilityReasonDto("AGE", "UNKNOWN",
                        String.format("Age missing in candidate profile (Notification requires: %s–%s years)",
                                minAge != null ? minAge : 18, maxAge != null ? maxAge : "Any")));
            } else {
                if (minAge != null && candAge < minAge) {
                    hasFail = true;
                    reasons.add(new EligibilityReasonDto("AGE", "FAIL",
                            String.format("Candidate age (%d) is below the minimum required age (%d)", candAge, minAge)));
                } else if (maxAge != null && candAge > maxAge) {
                    hasFail = true;
                    reasons.add(new EligibilityReasonDto("AGE", "FAIL",
                            String.format("Candidate age (%d) exceeds the maximum permissible age (%d)", candAge, maxAge)));
                } else {
                    reasons.add(new EligibilityReasonDto("AGE", "PASS",
                            String.format("Age eligible: Candidate age %d satisfies required limits (%s–%s)",
                                    candAge, minAge != null ? minAge : "18", maxAge != null ? maxAge : "Max")));
                }
            }
        } else {
            reasons.add(new EligibilityReasonDto("AGE", "PASS", "No specific age constraints specified"));
        }

        // 2. GENDER MATCHING (Strict, no assumptions - Section 17 & 19)
        GenderEligibility genderReq = eligibility.getGender();
        String candGender = profile.getGender() != null ? profile.getGender().trim().toUpperCase() : null;

        if (genderReq == GenderEligibility.FEMALE_ONLY) {
            if (candGender == null) {
                hasUnknown = true;
                missingFields.add("Gender");
                reasons.add(new EligibilityReasonDto("GENDER", "UNKNOWN", "Notification strictly specifies Women only; gender missing in candidate profile"));
            } else if (candGender.contains("FEMALE") || candGender.contains("WOMAN")) {
                reasons.add(new EligibilityReasonDto("GENDER", "PASS", "Gender eligible: Position reserved for Female candidates"));
            } else {
                hasFail = true;
                reasons.add(new EligibilityReasonDto("GENDER", "FAIL",
                        String.format("Notification strictly specifies Women only; candidate profile is '%s'", profile.getGender())));
            }
        } else if (genderReq == GenderEligibility.MALE_ONLY) {
            if (candGender == null) {
                hasUnknown = true;
                missingFields.add("Gender");
                reasons.add(new EligibilityReasonDto("GENDER", "UNKNOWN", "Notification strictly specifies Men only; gender missing in candidate profile"));
            } else if (candGender.contains("MALE") && !candGender.contains("FEMALE")) {
                reasons.add(new EligibilityReasonDto("GENDER", "PASS", "Gender eligible: Position reserved for Male candidates"));
            } else {
                hasFail = true;
                reasons.add(new EligibilityReasonDto("GENDER", "FAIL",
                        String.format("Notification strictly specifies Men only; candidate profile is '%s'", profile.getGender())));
            }
        } else {
            reasons.add(new EligibilityReasonDto("GENDER", "PASS", "Gender open to all candidates"));
        }

        // 3. EDUCATION MATCHING
        List<String> requiredEducations = parseJsonList(eligibility.getEducationJson());
        String candHighestEdu = profile.getHighestEducation() != null ? profile.getHighestEducation().trim().toUpperCase() : null;
        List<String> candDegrees = parseJsonList(profile.getDegreesJson());

        if (!requiredEducations.isEmpty()) {
            if (candHighestEdu == null && (candDegrees == null || candDegrees.isEmpty())) {
                hasUnknown = true;
                missingFields.add("Education");
                reasons.add(new EligibilityReasonDto("EDUCATION", "UNKNOWN",
                        String.format("Education details missing in profile (Notification requires: %s)", String.join(", ", requiredEducations))));
            } else {
                boolean matched = evaluateEducationMatch(candHighestEdu, candDegrees, requiredEducations);
                if (matched) {
                    reasons.add(new EligibilityReasonDto("EDUCATION", "PASS",
                            String.format("Education eligible: Candidate qualification satisfies '%s'", String.join(" / ", requiredEducations))));
                } else {
                    hasFail = true;
                    reasons.add(new EligibilityReasonDto("EDUCATION", "FAIL",
                            String.format("Education requirement '%s' not satisfied by candidate's qualification (%s)",
                                    String.join(" / ", requiredEducations), profile.getHighestEducation())));
                }
            }
        } else {
            reasons.add(new EligibilityReasonDto("EDUCATION", "PASS", "No mandatory educational qualification restriction specified"));
        }

        // 4. DOMICILE / LOCATION MATCHING
        String reqDomicile = eligibility.getDomicile();
        if (reqDomicile != null && !reqDomicile.isBlank() && !reqDomicile.equalsIgnoreCase("NOT_SPECIFIED") && !reqDomicile.equalsIgnoreCase("ALL-INDIA")) {
            String candDomicileState = profile.getDomicileState() != null ? profile.getDomicileState() : profile.getState();
            String candDomicileDistrict = profile.getDomicileDistrict() != null ? profile.getDomicileDistrict() : profile.getDistrict();

            if (candDomicileState == null && candDomicileDistrict == null) {
                hasUnknown = true;
                missingFields.add("Domicile");
                reasons.add(new EligibilityReasonDto("DOMICILE", "UNKNOWN",
                        String.format("Domicile requirement '%s' cannot be confirmed (profile domicile missing)", reqDomicile)));
            } else {
                boolean domicileMatch = checkDomicileMatch(reqDomicile, candDomicileState, candDomicileDistrict);
                if (domicileMatch) {
                    reasons.add(new EligibilityReasonDto("DOMICILE", "PASS",
                            String.format("Domicile eligible: Candidate resident criteria matches '%s'", reqDomicile)));
                } else {
                    // Domicile mismatch
                    hasFail = true;
                    reasons.add(new EligibilityReasonDto("DOMICILE", "FAIL",
                            String.format("Domicile requirement: Position restricted to '%s' (Candidate domicile is %s, %s)",
                                    reqDomicile, candDomicileDistrict != null ? candDomicileDistrict : "N/A", candDomicileState != null ? candDomicileState : "N/A")));
                }
            }
        } else {
            reasons.add(new EligibilityReasonDto("DOMICILE", "PASS", "Open to all Indian residents"));
        }

        // 5. EXPERIENCE MATCHING
        BigDecimal minExp = eligibility.getExperienceYearsMin();
        if (minExp != null && minExp.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal candExp = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience() : BigDecimal.ZERO;
            if (candExp.compareTo(minExp) < 0) {
                hasFail = true;
                reasons.add(new EligibilityReasonDto("EXPERIENCE", "FAIL",
                        String.format("Requires %s years of experience; candidate profile has %s years", minExp, candExp)));
            } else {
                reasons.add(new EligibilityReasonDto("EXPERIENCE", "PASS",
                        String.format("Experience eligible: Candidate has %s years (Requires %s years)", candExp, minExp)));
            }
        } else {
            reasons.add(new EligibilityReasonDto("EXPERIENCE", "PASS", "No prior experience required (Fresher eligible)"));
        }

        // Compute overall status
        EligibilityStatus overallStatus;
        String summary;

        if (hasFail) {
            overallStatus = EligibilityStatus.NOT_ELIGIBLE;
            summary = "Not eligible based on official notification criteria";
        } else if (hasUnknown) {
            if (missingFields.size() > 2) {
                overallStatus = EligibilityStatus.UNKNOWN;
                summary = "Missing profile information: " + String.join(", ", missingFields);
            } else {
                overallStatus = EligibilityStatus.LIKELY_ELIGIBLE;
                summary = "Likely eligible; verify " + String.join(", ", missingFields);
            }
        } else {
            overallStatus = EligibilityStatus.ELIGIBLE;
            summary = "Fully eligible according to official notification criteria";
        }

        return new EligibilityEvaluationResultDto(overallStatus, summary, reasons, missingFields);
    }

    private boolean evaluateEducationMatch(String candHighestEdu, List<String> candDegrees, List<String> requiredEducations) {
        if (requiredEducations == null || requiredEducations.isEmpty()) return true;

        for (String req : requiredEducations) {
            String normReq = req.trim().toUpperCase();
            if (normReq.contains("ANY") || normReq.contains("NOT_SPECIFIED")) return true;

            // Direct string match against highest edu or degrees
            if (candHighestEdu != null && (candHighestEdu.contains(normReq) || normReq.contains(candHighestEdu))) {
                return true;
            }
            if (candDegrees != null) {
                for (String deg : candDegrees) {
                    if (deg != null && (deg.toUpperCase().contains(normReq) || normReq.contains(deg.toUpperCase()))) {
                        return true;
                    }
                }
            }

            // Hierarchy level check
            Integer reqLevel = getEducationLevel(normReq);
            Integer candLevel = getEducationLevel(candHighestEdu);

            if (reqLevel != null && candLevel != null) {
                if (candLevel >= reqLevel) {
                    return true;
                }
            }
        }
        return false;
    }

    private Integer getEducationLevel(String text) {
        if (text == null) return null;
        String upper = text.toUpperCase().trim();
        for (Map.Entry<String, Integer> entry : EDUCATION_LEVELS.entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean checkDomicileMatch(String reqDomicile, String candState, String candDistrict) {
        String reqUpper = reqDomicile.toUpperCase();
        if (candState != null && reqUpper.contains(candState.toUpperCase())) {
            return true;
        }
        if (candDistrict != null && reqUpper.contains(candDistrict.toUpperCase())) {
            return true;
        }
        return false;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
