package com.jobhunter.dto.tailoring;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TailoringPlanDto {

    private UUID targetJobId;
    private String targetJobTitle;
    private String targetCompany;
    private String targetRole;
    private List<String> resumeSectionsAffected = new ArrayList<>();
    private List<String> skillsToEmphasize = new ArrayList<>();
    private List<String> skillsToDeemphasize = new ArrayList<>();
    private List<String> projectsToEmphasize = new ArrayList<>();
    private List<String> projectsToDeemphasize = new ArrayList<>();
    private List<BulletTailoringItem> bulletSharpeningProposals = new ArrayList<>();
    private List<String> recommendedSectionOrder = new ArrayList<>();
    private String sectionOrderRationale;
    private List<AtsKeywordItem> atsTerminology = new ArrayList<>();
    private List<RejectedKeywordItem> rejectedKeywords = new ArrayList<>();
    private List<String> evidenceReferences = new ArrayList<>();
    private String overallStrategy;

    public TailoringPlanDto() {}

    public UUID getTargetJobId() { return targetJobId; }
    public void setTargetJobId(UUID targetJobId) { this.targetJobId = targetJobId; }

    public String getTargetJobTitle() { return targetJobTitle; }
    public void setTargetJobTitle(String targetJobTitle) { this.targetJobTitle = targetJobTitle; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public List<String> getResumeSectionsAffected() { return resumeSectionsAffected; }
    public void setResumeSectionsAffected(List<String> resumeSectionsAffected) { this.resumeSectionsAffected = resumeSectionsAffected; }

    public List<String> getSkillsToEmphasize() { return skillsToEmphasize; }
    public void setSkillsToEmphasize(List<String> skillsToEmphasize) { this.skillsToEmphasize = skillsToEmphasize; }

    public List<String> getSkillsToDeemphasize() { return skillsToDeemphasize; }
    public void setSkillsToDeemphasize(List<String> skillsToDeemphasize) { this.skillsToDeemphasize = skillsToDeemphasize; }

    public List<String> getProjectsToEmphasize() { return projectsToEmphasize; }
    public void setProjectsToEmphasize(List<String> projectsToEmphasize) { this.projectsToEmphasize = projectsToEmphasize; }

    public List<String> getProjectsToDeemphasize() { return projectsToDeemphasize; }
    public void setProjectsToDeemphasize(List<String> projectsToDeemphasize) { this.projectsToDeemphasize = projectsToDeemphasize; }

    public List<BulletTailoringItem> getBulletSharpeningProposals() { return bulletSharpeningProposals; }
    public void setBulletSharpeningProposals(List<BulletTailoringItem> bulletSharpeningProposals) { this.bulletSharpeningProposals = bulletSharpeningProposals; }

    public List<String> getRecommendedSectionOrder() { return recommendedSectionOrder; }
    public void setRecommendedSectionOrder(List<String> recommendedSectionOrder) { this.recommendedSectionOrder = recommendedSectionOrder; }

    public String getSectionOrderRationale() { return sectionOrderRationale; }
    public void setSectionOrderRationale(String sectionOrderRationale) { this.sectionOrderRationale = sectionOrderRationale; }

    public List<AtsKeywordItem> getAtsTerminology() { return atsTerminology; }
    public void setAtsTerminology(List<AtsKeywordItem> atsTerminology) { this.atsTerminology = atsTerminology; }

    public List<RejectedKeywordItem> getRejectedKeywords() { return rejectedKeywords; }
    public void setRejectedKeywords(List<RejectedKeywordItem> rejectedKeywords) { this.rejectedKeywords = rejectedKeywords; }

    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }

    public String getOverallStrategy() { return overallStrategy; }
    public void setOverallStrategy(String overallStrategy) { this.overallStrategy = overallStrategy; }
}
