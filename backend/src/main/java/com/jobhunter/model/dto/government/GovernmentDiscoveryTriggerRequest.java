package com.jobhunter.model.dto.government;

public class GovernmentDiscoveryTriggerRequest {
    private String state;
    private String district;
    private String sourceType;
    private String searchKeyword;
    private int maxQueries = 5;

    public GovernmentDiscoveryTriggerRequest() {}

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }

    public int getMaxQueries() { return maxQueries; }
    public void setMaxQueries(int maxQueries) { this.maxQueries = maxQueries; }
}
