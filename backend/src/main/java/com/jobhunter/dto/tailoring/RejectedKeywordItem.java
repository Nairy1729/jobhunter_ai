package com.jobhunter.dto.tailoring;

public class RejectedKeywordItem {

    private String keyword;
    private String reason;

    public RejectedKeywordItem() {}

    public RejectedKeywordItem(String keyword, String reason) {
        this.keyword = keyword;
        this.reason = reason;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
