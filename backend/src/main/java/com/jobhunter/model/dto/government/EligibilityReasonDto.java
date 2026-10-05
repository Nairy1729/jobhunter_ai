package com.jobhunter.model.dto.government;

public class EligibilityReasonDto {
    private String criterion;      // AGE, GENDER, EDUCATION, DOMICILE, CATEGORY, EXPERIENCE, PWD, EX_SERVICEMAN
    private String status;         // PASS, FAIL, UNKNOWN
    private String explanation;

    public EligibilityReasonDto() {}

    public EligibilityReasonDto(String criterion, String status, String explanation) {
        this.criterion = criterion;
        this.status = status;
        this.explanation = explanation;
    }

    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
