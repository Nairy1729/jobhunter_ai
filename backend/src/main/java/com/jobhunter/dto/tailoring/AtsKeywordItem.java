package com.jobhunter.dto.tailoring;

public class AtsKeywordItem {

    private String keyword;
    private String status; // SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED
    private String context;
    private String evidenceReference;

    public AtsKeywordItem() {}

    public AtsKeywordItem(String keyword, String status, String context, String evidenceReference) {
        this.keyword = keyword;
        this.status = status;
        this.context = context;
        this.evidenceReference = evidenceReference;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getEvidenceReference() { return evidenceReference; }
    public void setEvidenceReference(String evidenceReference) { this.evidenceReference = evidenceReference; }
}
