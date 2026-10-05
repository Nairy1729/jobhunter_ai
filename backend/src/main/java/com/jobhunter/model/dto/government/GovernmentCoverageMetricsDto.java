package com.jobhunter.model.dto.government;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class GovernmentCoverageMetricsDto {
    private long totalSources;
    private long activeSources;
    private long failedSources;
    private Instant lastSuccessfulCrawl;

    // Breakdown by source type
    private long centralGovernment;
    private long stateGovernment;
    private long districtAdministration;
    private long municipal;
    private long panchayat;
    private long universities;
    private long psus;
    private long departments;
    private long health;
    private long education;
    private long womenChildDevelopment;
    private long other;

    // Jobs summary
    private long totalJobs;
    private long verifiedOfficialJobs;
    private long contractualSamvidaJobs;
    private long smallLocalJobs;
    private long openJobs;

    private Map<String, Long> employmentTypeBreakdown = new HashMap<>();
    private Map<String, Long> verificationStatusBreakdown = new HashMap<>();

    public GovernmentCoverageMetricsDto() {}

    // Getters and Setters
    public long getTotalSources() { return totalSources; }
    public void setTotalSources(long totalSources) { this.totalSources = totalSources; }

    public long getActiveSources() { return activeSources; }
    public void setActiveSources(long activeSources) { this.activeSources = activeSources; }

    public long getFailedSources() { return failedSources; }
    public void setFailedSources(long failedSources) { this.failedSources = failedSources; }

    public Instant getLastSuccessfulCrawl() { return lastSuccessfulCrawl; }
    public void setLastSuccessfulCrawl(Instant lastSuccessfulCrawl) { this.lastSuccessfulCrawl = lastSuccessfulCrawl; }

    public long getCentralGovernment() { return centralGovernment; }
    public void setCentralGovernment(long centralGovernment) { this.centralGovernment = centralGovernment; }

    public long getStateGovernment() { return stateGovernment; }
    public void setStateGovernment(long stateGovernment) { this.stateGovernment = stateGovernment; }

    public long getDistrictAdministration() { return districtAdministration; }
    public void setDistrictAdministration(long districtAdministration) { this.districtAdministration = districtAdministration; }

    public long getMunicipal() { return municipal; }
    public void setMunicipal(long municipal) { this.municipal = municipal; }

    public long getPanchayat() { return panchayat; }
    public void setPanchayat(long panchayat) { this.panchayat = panchayat; }

    public long getUniversities() { return universities; }
    public void setUniversities(long universities) { this.universities = universities; }

    public long getPsus() { return psus; }
    public void setPsus(long psus) { this.psus = psus; }

    public long getDepartments() { return departments; }
    public void setDepartments(long departments) { this.departments = departments; }

    public long getHealth() { return health; }
    public void setHealth(long health) { this.health = health; }

    public long getEducation() { return education; }
    public void setEducation(long education) { this.education = education; }

    public long getWomenChildDevelopment() { return womenChildDevelopment; }
    public void setWomenChildDevelopment(long womenChildDevelopment) { this.womenChildDevelopment = womenChildDevelopment; }

    public long getOther() { return other; }
    public void setOther(long other) { this.other = other; }

    public long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }

    public long getVerifiedOfficialJobs() { return verifiedOfficialJobs; }
    public void setVerifiedOfficialJobs(long verifiedOfficialJobs) { this.verifiedOfficialJobs = verifiedOfficialJobs; }

    public long getContractualSamvidaJobs() { return contractualSamvidaJobs; }
    public void setContractualSamvidaJobs(long contractualSamvidaJobs) { this.contractualSamvidaJobs = contractualSamvidaJobs; }

    public long getSmallLocalJobs() { return smallLocalJobs; }
    public void setSmallLocalJobs(long smallLocalJobs) { this.smallLocalJobs = smallLocalJobs; }

    public long getOpenJobs() { return openJobs; }
    public void setOpenJobs(long openJobs) { this.openJobs = openJobs; }

    public Map<String, Long> getEmploymentTypeBreakdown() { return employmentTypeBreakdown; }
    public void setEmploymentTypeBreakdown(Map<String, Long> employmentTypeBreakdown) { this.employmentTypeBreakdown = employmentTypeBreakdown; }

    public Map<String, Long> getVerificationStatusBreakdown() { return verificationStatusBreakdown; }
    public void setVerificationStatusBreakdown(Map<String, Long> verificationStatusBreakdown) { this.verificationStatusBreakdown = verificationStatusBreakdown; }
}
