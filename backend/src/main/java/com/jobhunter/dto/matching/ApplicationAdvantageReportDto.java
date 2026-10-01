package com.jobhunter.dto.matching;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

/**
 * 10-Dimension Application Advantage Report ("The Edge")
 *
 * Dedicated strictly to application positioning and strategic advantage:
 * 1. Employer Priorities
 * 2. Candidate Relevance
 * 3. Strongest Evidence
 * 4. What to Emphasize
 * 5. What to De-emphasize
 * 6. Honest Gaps
 * 7. Transferable Skills
 * 8. Resume Positioning
 * 9. Application Fit & Positioning
 * 10. Application Strategy
 */
public class ApplicationAdvantageReportDto {

    private List<String> employerPriorities = new ArrayList<>();
    private String candidateRelevance;
    private List<String> strongestEvidence = new ArrayList<>();
    private List<String> whatToEmphasize = new ArrayList<>();
    private List<String> whatToDeemphasize = new ArrayList<>();

    @JsonAlias({"gapsAndMitigations"})
    private List<String> honestGaps = new ArrayList<>();

    private List<String> transferableSkills = new ArrayList<>();
    private List<String> resumePositioning = new ArrayList<>();
    private List<String> applicationFitAndPositioning = new ArrayList<>();
    private String applicationStrategy;

    public ApplicationAdvantageReportDto() {}

    public List<String> getEmployerPriorities() { return employerPriorities; }
    public void setEmployerPriorities(List<String> employerPriorities) { this.employerPriorities = employerPriorities; }

    public String getCandidateRelevance() { return candidateRelevance; }
    public void setCandidateRelevance(String candidateRelevance) { this.candidateRelevance = candidateRelevance; }

    public List<String> getStrongestEvidence() { return strongestEvidence; }
    public void setStrongestEvidence(List<String> strongestEvidence) { this.strongestEvidence = strongestEvidence; }

    public List<String> getWhatToEmphasize() { return whatToEmphasize; }
    public void setWhatToEmphasize(List<String> whatToEmphasize) { this.whatToEmphasize = whatToEmphasize; }

    public List<String> getWhatToDeemphasize() { return whatToDeemphasize; }
    public void setWhatToDeemphasize(List<String> whatToDeemphasize) { this.whatToDeemphasize = whatToDeemphasize; }

    public List<String> getHonestGaps() { return honestGaps; }
    public void setHonestGaps(List<String> honestGaps) { this.honestGaps = honestGaps; }

    // Backward-compatible alias for existing JSON records in database
    public List<String> getGapsAndMitigations() { return honestGaps; }
    public void setGapsAndMitigations(List<String> gapsAndMitigations) { this.honestGaps = gapsAndMitigations; }

    public List<String> getTransferableSkills() { return transferableSkills; }
    public void setTransferableSkills(List<String> transferableSkills) { this.transferableSkills = transferableSkills; }

    public List<String> getResumePositioning() { return resumePositioning; }
    public void setResumePositioning(List<String> resumePositioning) { this.resumePositioning = resumePositioning; }

    public List<String> getApplicationFitAndPositioning() { return applicationFitAndPositioning; }
    public void setApplicationFitAndPositioning(List<String> applicationFitAndPositioning) { this.applicationFitAndPositioning = applicationFitAndPositioning; }

    public String getApplicationStrategy() { return applicationStrategy; }
    public void setApplicationStrategy(String applicationStrategy) { this.applicationStrategy = applicationStrategy; }
}
