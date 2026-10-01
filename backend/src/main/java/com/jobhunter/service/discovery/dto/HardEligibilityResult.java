package com.jobhunter.service.discovery.dto;

public class HardEligibilityResult {

    private final boolean eligible;
    private final String reason;
    private final String mismatchCategory;

    public HardEligibilityResult(boolean eligible, String reason, String mismatchCategory) {
        this.eligible = eligible;
        this.reason = reason;
        this.mismatchCategory = mismatchCategory;
    }

    public static HardEligibilityResult pass() {
        return new HardEligibilityResult(true, "Satisfies all hard eligibility requirements.", "NONE");
    }

    public static HardEligibilityResult reject(String reason, String mismatchCategory) {
        return new HardEligibilityResult(false, reason, mismatchCategory);
    }

    public boolean isEligible() { return eligible; }
    public String getReason() { return reason; }
    public String getMismatchCategory() { return mismatchCategory; }
}
