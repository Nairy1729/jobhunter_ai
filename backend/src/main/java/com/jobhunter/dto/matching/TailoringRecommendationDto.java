package com.jobhunter.dto.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TailoringRecommendationDto {

    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private List<String> sectionsToReorder = new ArrayList<>();
    private List<String> skillsToFeature = new ArrayList<>();
    private List<String> skillsToDeemphasize = new ArrayList<>();
    private List<BulletSharpeningProposal> bulletSharpeningProposals = new ArrayList<>();
    private String rationale;

    public static class BulletSharpeningProposal {
        private String originalBullet;
        private String proposedBullet;
        private String groundedEvidenceReference;
        private String atsKeywordRationale;

        public BulletSharpeningProposal() {}

        public BulletSharpeningProposal(String originalBullet, String proposedBullet,
                                        String groundedEvidenceReference, String atsKeywordRationale) {
            this.originalBullet = originalBullet;
            this.proposedBullet = proposedBullet;
            this.groundedEvidenceReference = groundedEvidenceReference;
            this.atsKeywordRationale = atsKeywordRationale;
        }

        public String getOriginalBullet() { return originalBullet; }
        public void setOriginalBullet(String originalBullet) { this.originalBullet = originalBullet; }

        public String getProposedBullet() { return proposedBullet; }
        public void setProposedBullet(String proposedBullet) { this.proposedBullet = proposedBullet; }

        public String getGroundedEvidenceReference() { return groundedEvidenceReference; }
        public void setGroundedEvidenceReference(String groundedEvidenceReference) { this.groundedEvidenceReference = groundedEvidenceReference; }

        public String getAtsKeywordRationale() { return atsKeywordRationale; }
        public void setAtsKeywordRationale(String atsKeywordRationale) { this.atsKeywordRationale = atsKeywordRationale; }
    }

    public TailoringRecommendationDto() {}

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public List<String> getSectionsToReorder() { return sectionsToReorder; }
    public void setSectionsToReorder(List<String> sectionsToReorder) { this.sectionsToReorder = sectionsToReorder; }

    public List<String> getSkillsToFeature() { return skillsToFeature; }
    public void setSkillsToFeature(List<String> skillsToFeature) { this.skillsToFeature = skillsToFeature; }

    public List<String> getSkillsToDeemphasize() { return skillsToDeemphasize; }
    public void setSkillsToDeemphasize(List<String> skillsToDeemphasize) { this.skillsToDeemphasize = skillsToDeemphasize; }

    public List<BulletSharpeningProposal> getBulletSharpeningProposals() { return bulletSharpeningProposals; }
    public void setBulletSharpeningProposals(List<BulletSharpeningProposal> bulletSharpeningProposals) { this.bulletSharpeningProposals = bulletSharpeningProposals; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
