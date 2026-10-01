package com.jobhunter.dto.tailoring;

import java.util.ArrayList;
import java.util.List;

public class BulletTailoringItem {

    private String originalBullet;
    private String proposedBullet;
    private String reason;
    private String jobRequirement;
    private List<String> evidenceReferences = new ArrayList<>();
    private String groundingStatus; // VERIFIED_GROUNDED, ADJUSTED, REJECTED

    public BulletTailoringItem() {}

    public BulletTailoringItem(String originalBullet, String proposedBullet, String reason,
                               String jobRequirement, List<String> evidenceReferences, String groundingStatus) {
        this.originalBullet = originalBullet;
        this.proposedBullet = proposedBullet;
        this.reason = reason;
        this.jobRequirement = jobRequirement;
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.groundingStatus = groundingStatus;
    }

    public String getOriginalBullet() { return originalBullet; }
    public void setOriginalBullet(String originalBullet) { this.originalBullet = originalBullet; }

    public String getProposedBullet() { return proposedBullet; }
    public void setProposedBullet(String proposedBullet) { this.proposedBullet = proposedBullet; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getJobRequirement() { return jobRequirement; }
    public void setJobRequirement(String jobRequirement) { this.jobRequirement = jobRequirement; }

    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }

    public String getGroundingStatus() { return groundingStatus; }
    public void setGroundingStatus(String groundingStatus) { this.groundingStatus = groundingStatus; }
}
