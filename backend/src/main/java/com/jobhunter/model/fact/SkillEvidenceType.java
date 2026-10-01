package com.jobhunter.model.fact;

public enum SkillEvidenceType {
    VERIFIED_SKILL,
    PROJECT_SKILL,
    TRANSFERABLE_SKILL,
    UNVERIFIED_SKILL;

    public boolean isAllowedOnResume() {
        return this == VERIFIED_SKILL || this == PROJECT_SKILL;
    }
}
