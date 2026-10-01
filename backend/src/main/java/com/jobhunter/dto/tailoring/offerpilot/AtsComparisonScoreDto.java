package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AtsComparisonScoreDto {

    private double masterOverallScore;
    private double masterRequiredSkillScore;
    private double masterPreferredSkillScore;
    private double masterKeywordScore;
    private double masterCompletenessScore;

    private double tailoredOverallScore;
    private double tailoredRequiredSkillScore;
    private double tailoredPreferredSkillScore;
    private double tailoredKeywordScore;
    private double tailoredCompletenessScore;

    private double scoreDelta;
    private int matchedKeywordsDelta;
    private List<String> newlyAlignedKeywords = new ArrayList<>();
    private List<String> remainingGaps = new ArrayList<>();

    public AtsComparisonScoreDto() {}

    public double getMasterOverallScore() { return masterOverallScore; }
    public void setMasterOverallScore(double masterOverallScore) { this.masterOverallScore = masterOverallScore; }

    public double getMasterRequiredSkillScore() { return masterRequiredSkillScore; }
    public void setMasterRequiredSkillScore(double masterRequiredSkillScore) { this.masterRequiredSkillScore = masterRequiredSkillScore; }

    public double getMasterPreferredSkillScore() { return masterPreferredSkillScore; }
    public void setMasterPreferredSkillScore(double masterPreferredSkillScore) { this.masterPreferredSkillScore = masterPreferredSkillScore; }

    public double getMasterKeywordScore() { return masterKeywordScore; }
    public void setMasterKeywordScore(double masterKeywordScore) { this.masterKeywordScore = masterKeywordScore; }

    public double getMasterCompletenessScore() { return masterCompletenessScore; }
    public void setMasterCompletenessScore(double masterCompletenessScore) { this.masterCompletenessScore = masterCompletenessScore; }

    public double getTailoredOverallScore() { return tailoredOverallScore; }
    public void setTailoredOverallScore(double tailoredOverallScore) { this.tailoredOverallScore = tailoredOverallScore; }

    public double getTailoredRequiredSkillScore() { return tailoredRequiredSkillScore; }
    public void setTailoredRequiredSkillScore(double tailoredRequiredSkillScore) { this.tailoredRequiredSkillScore = tailoredRequiredSkillScore; }

    public double getTailoredPreferredSkillScore() { return tailoredPreferredSkillScore; }
    public void setTailoredPreferredSkillScore(double tailoredPreferredSkillScore) { this.tailoredPreferredSkillScore = tailoredPreferredSkillScore; }

    public double getTailoredKeywordScore() { return tailoredKeywordScore; }
    public void setTailoredKeywordScore(double tailoredKeywordScore) { this.tailoredKeywordScore = tailoredKeywordScore; }

    public double getTailoredCompletenessScore() { return tailoredCompletenessScore; }
    public void setTailoredCompletenessScore(double tailoredCompletenessScore) { this.tailoredCompletenessScore = tailoredCompletenessScore; }

    public double getScoreDelta() { return scoreDelta; }
    public void setScoreDelta(double scoreDelta) { this.scoreDelta = scoreDelta; }

    public int getMatchedKeywordsDelta() { return matchedKeywordsDelta; }
    public void setMatchedKeywordsDelta(int matchedKeywordsDelta) { this.matchedKeywordsDelta = matchedKeywordsDelta; }

    public List<String> getNewlyAlignedKeywords() { return newlyAlignedKeywords; }
    public void setNewlyAlignedKeywords(List<String> newlyAlignedKeywords) { this.newlyAlignedKeywords = newlyAlignedKeywords; }

    public List<String> getRemainingGaps() { return remainingGaps; }
    public void setRemainingGaps(List<String> remainingGaps) { this.remainingGaps = remainingGaps; }
}
