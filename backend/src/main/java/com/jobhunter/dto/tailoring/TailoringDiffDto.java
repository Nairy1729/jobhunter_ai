package com.jobhunter.dto.tailoring;

import java.util.ArrayList;
import java.util.List;

public class TailoringDiffDto {

    private String masterResumeContent;
    private String tailoredResumeContent;
    private List<String> addedEmphasis = new ArrayList<>();
    private List<String> removedOrDeemphasized = new ArrayList<>();
    private List<String> reorderedSections = new ArrayList<>();
    private List<BulletTailoringItem> modifiedBullets = new ArrayList<>();
    private List<AtsKeywordItem> atsTerminologyChanges = new ArrayList<>();
    private List<RejectedKeywordItem> rejectedKeywords = new ArrayList<>();

    public TailoringDiffDto() {}

    public String getMasterResumeContent() { return masterResumeContent; }
    public void setMasterResumeContent(String masterResumeContent) { this.masterResumeContent = masterResumeContent; }

    public String getTailoredResumeContent() { return tailoredResumeContent; }
    public void setTailoredResumeContent(String tailoredResumeContent) { this.tailoredResumeContent = tailoredResumeContent; }

    public List<String> getAddedEmphasis() { return addedEmphasis; }
    public void setAddedEmphasis(List<String> addedEmphasis) { this.addedEmphasis = addedEmphasis; }

    public List<String> getRemovedOrDeemphasized() { return removedOrDeemphasized; }
    public void setRemovedOrDeemphasized(List<String> removedOrDeemphasized) { this.removedOrDeemphasized = removedOrDeemphasized; }

    public List<String> getReorderedSections() { return reorderedSections; }
    public void setReorderedSections(List<String> reorderedSections) { this.reorderedSections = reorderedSections; }

    public List<BulletTailoringItem> getModifiedBullets() { return modifiedBullets; }
    public void setModifiedBullets(List<BulletTailoringItem> modifiedBullets) { this.modifiedBullets = modifiedBullets; }

    public List<AtsKeywordItem> getAtsTerminologyChanges() { return atsTerminologyChanges; }
    public void setAtsTerminologyChanges(List<AtsKeywordItem> atsTerminologyChanges) { this.atsTerminologyChanges = atsTerminologyChanges; }

    public List<RejectedKeywordItem> getRejectedKeywords() { return rejectedKeywords; }
    public void setRejectedKeywords(List<RejectedKeywordItem> rejectedKeywords) { this.rejectedKeywords = rejectedKeywords; }
}
