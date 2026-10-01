package com.jobhunter.service.tailoring.offerpilot;

import com.jobhunter.dto.tailoring.offerpilot.AtsComparisonScoreDto;
import com.jobhunter.dto.tailoring.offerpilot.TailoredResumePayload;
import com.jobhunter.model.entity.Job;
import com.jobhunter.model.entity.JobRequirement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Closed-Loop ATS Scoring & Comparison Service.
 * Implements Section 10 of the OfferPilot Architecture Blueprint.
 */
@Service
public class AtsScoringService {

    /**
     * Computes closed-loop comparison between Master Resume and Tailored Resume against target Job.
     */
    public AtsComparisonScoreDto computeComparison(
            String masterResumeText,
            TailoredResumePayload.TailoredResumeContent tailoredContent,
            Job job,
            List<String> requiredSkills,
            List<String> preferredSkills,
            List<String> keywords) {

        AtsComparisonScoreDto dto = new AtsComparisonScoreDto();
        String masterLower = masterResumeText != null ? masterResumeText.toLowerCase() : "";
        String tailoredText = extractTextFromTailoredContent(tailoredContent).toLowerCase();

        // 1. Required Skills Scoring
        Set<String> masterReqMatched = new HashSet<>();
        Set<String> tailoredReqMatched = new HashSet<>();
        for (String req : requiredSkills) {
            String norm = req.toLowerCase();
            if (masterLower.contains(norm)) masterReqMatched.add(req);
            if (tailoredText.contains(norm)) tailoredReqMatched.add(req);
        }
        double masterReqScore = requiredSkills.isEmpty() ? 100.0 : (double) masterReqMatched.size() / requiredSkills.size() * 100.0;
        double tailoredReqScore = requiredSkills.isEmpty() ? 100.0 : (double) tailoredReqMatched.size() / requiredSkills.size() * 100.0;

        // 2. Preferred Skills Scoring
        Set<String> masterPrefMatched = new HashSet<>();
        Set<String> tailoredPrefMatched = new HashSet<>();
        for (String pref : preferredSkills) {
            String norm = pref.toLowerCase();
            if (masterLower.contains(norm)) masterPrefMatched.add(pref);
            if (tailoredText.contains(norm)) tailoredPrefMatched.add(pref);
        }
        double masterPrefScore = preferredSkills.isEmpty() ? 100.0 : (double) masterPrefMatched.size() / preferredSkills.size() * 100.0;
        double tailoredPrefScore = preferredSkills.isEmpty() ? 100.0 : (double) tailoredPrefMatched.size() / preferredSkills.size() * 100.0;

        // 3. Keyword Scoring
        Set<String> masterKwMatched = new HashSet<>();
        Set<String> tailoredKwMatched = new HashSet<>();
        for (String kw : keywords) {
            String norm = kw.toLowerCase();
            if (masterLower.contains(norm)) masterKwMatched.add(kw);
            if (tailoredText.contains(norm)) tailoredKwMatched.add(kw);
        }
        double masterKwScore = keywords.isEmpty() ? 100.0 : (double) masterKwMatched.size() / keywords.size() * 100.0;
        double tailoredKwScore = keywords.isEmpty() ? 100.0 : (double) tailoredKwMatched.size() / keywords.size() * 100.0;

        // 4. Section Completeness Scoring
        double masterCompleteness = evaluateCompleteness(masterLower);
        double tailoredCompleteness = evaluateCompleteness(tailoredText);

        // 5. Overall ATS Score calculation
        // Formula: 40% Required Skills + 20% Preferred Skills + 25% Keywords + 15% Section Completeness
        double masterOverall = round(masterReqScore * 0.40 + masterPrefScore * 0.20 + masterKwScore * 0.25 + masterCompleteness * 0.15);
        double tailoredOverall = round(tailoredReqScore * 0.40 + tailoredPrefScore * 0.20 + tailoredKwScore * 0.25 + tailoredCompleteness * 0.15);

        dto.setMasterOverallScore(masterOverall);
        dto.setMasterRequiredSkillScore(round(masterReqScore));
        dto.setMasterPreferredSkillScore(round(masterPrefScore));
        dto.setMasterKeywordScore(round(masterKwScore));
        dto.setMasterCompletenessScore(round(masterCompleteness));

        dto.setTailoredOverallScore(tailoredOverall);
        dto.setTailoredRequiredSkillScore(round(tailoredReqScore));
        dto.setTailoredPreferredSkillScore(round(tailoredPrefScore));
        dto.setTailoredKeywordScore(round(tailoredKwScore));
        dto.setTailoredCompletenessScore(round(tailoredCompleteness));

        // 6. Comparison Delta
        dto.setScoreDelta(round(tailoredOverall - masterOverall));

        Set<String> newlyAligned = new HashSet<>(tailoredKwMatched);
        newlyAligned.addAll(tailoredReqMatched);
        newlyAligned.removeAll(masterKwMatched);
        newlyAligned.removeAll(masterReqMatched);
        dto.setNewlyAlignedKeywords(new ArrayList<>(newlyAligned));
        dto.setMatchedKeywordsDelta(newlyAligned.size());

        // Remaining Gaps: Required & preferred skills neither present in master nor tailored
        Set<String> allReqAndPref = new HashSet<>(requiredSkills);
        allReqAndPref.addAll(preferredSkills);
        allReqAndPref.removeAll(tailoredReqMatched);
        allReqAndPref.removeAll(tailoredPrefMatched);
        dto.setRemainingGaps(new ArrayList<>(allReqAndPref));

        return dto;
    }

    private double evaluateCompleteness(String text) {
        if (text == null || text.isBlank()) return 0.0;
        int sectionsFound = 0;
        if (text.contains("summary") || text.contains("profile") || text.contains("objective")) sectionsFound++;
        if (text.contains("experience") || text.contains("employment") || text.contains("work")) sectionsFound++;
        if (text.contains("skill") || text.contains("technolog")) sectionsFound++;
        if (text.contains("project")) sectionsFound++;
        if (text.contains("education") || text.contains("degree") || text.contains("university")) sectionsFound++;

        return (sectionsFound / 5.0) * 100.0;
    }

    private String extractTextFromTailoredContent(TailoredResumePayload.TailoredResumeContent content) {
        if (content == null) return "";
        StringBuilder sb = new StringBuilder();
        if (content.getProfessionalSummary() != null) sb.append(content.getProfessionalSummary()).append(" ");
        if (content.getSkills() != null) {
            TailoredResumePayload.SkillsContainer s = content.getSkills();
            if (s.getProgrammingLanguages() != null) sb.append(String.join(" ", s.getProgrammingLanguages())).append(" ");
            if (s.getFrameworks() != null) sb.append(String.join(" ", s.getFrameworks())).append(" ");
            if (s.getDatabases() != null) sb.append(String.join(" ", s.getDatabases())).append(" ");
            if (s.getCloud() != null) sb.append(String.join(" ", s.getCloud())).append(" ");
            if (s.getTools() != null) sb.append(String.join(" ", s.getTools())).append(" ");
            if (s.getOther() != null) sb.append(String.join(" ", s.getOther())).append(" ");
        }
        if (content.getExperience() != null) {
            for (TailoredResumePayload.ExperienceEntry e : content.getExperience()) {
                if (e.getCompany() != null) sb.append(e.getCompany()).append(" ");
                if (e.getRole() != null) sb.append(e.getRole()).append(" ");
                if (e.getBullets() != null) sb.append(String.join(" ", e.getBullets())).append(" ");
            }
        }
        if (content.getProjects() != null) {
            for (TailoredResumePayload.ProjectEntry p : content.getProjects()) {
                if (p.getName() != null) sb.append(p.getName()).append(" ");
                if (p.getDescription() != null) sb.append(p.getDescription()).append(" ");
                if (p.getTechnologies() != null) sb.append(String.join(" ", p.getTechnologies())).append(" ");
                if (p.getBullets() != null) sb.append(String.join(" ", p.getBullets())).append(" ");
            }
        }
        return sb.toString();
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
