package com.jobhunter.dto.tailoring;

import java.util.ArrayList;
import java.util.List;

public class ResumeValidationReport {

    private boolean passed = true;
    private List<String> passedChecks = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> failedChecks = new ArrayList<>();
    private double qualityScore = 100.0;

    public ResumeValidationReport() {}

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public List<String> getPassedChecks() { return passedChecks; }
    public void setPassedChecks(List<String> passedChecks) { this.passedChecks = passedChecks; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getFailedChecks() { return failedChecks; }
    public void setFailedChecks(List<String> failedChecks) { this.failedChecks = failedChecks; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
}
