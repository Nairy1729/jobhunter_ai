package com.jobhunter.model.dto.government;

import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;

public class GovernmentJobFilterRequest {
    private String state;
    private String district;
    private GovernmentEmploymentType employmentType;
    private GovernmentJobStatus status = GovernmentJobStatus.OPEN;
    private GovernmentVerificationStatus verificationStatus;
    private boolean includeUnverified = false;
    private String education;
    private String gender;
    private String query;
    private String eligibilityFilter = "ALL"; // ALL, ELIGIBLE_ONLY, LIKELY_ELIGIBLE
    private int page = 0;
    private int size = 20;

    public GovernmentJobFilterRequest() {}

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public GovernmentEmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(GovernmentEmploymentType employmentType) { this.employmentType = employmentType; }

    public GovernmentJobStatus getStatus() { return status; }
    public void setStatus(GovernmentJobStatus status) { this.status = status; }

    public GovernmentVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(GovernmentVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public boolean isIncludeUnverified() { return includeUnverified; }
    public void setIncludeUnverified(boolean includeUnverified) { this.includeUnverified = includeUnverified; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getEligibilityFilter() { return eligibilityFilter; }
    public void setEligibilityFilter(String eligibilityFilter) { this.eligibilityFilter = eligibilityFilter; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
