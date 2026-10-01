-- JobHunter AI Schema Migration V6
-- Canonical Candidate Fact Store and Resume Tailoring Audit Log

CREATE TABLE IF NOT EXISTS candidate_facts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, 
    -- EDUCATION, EMPLOYMENT, ROLE, SKILL, TECHNOLOGY, PROJECT, CERTIFICATION, ACHIEVEMENT, RESPONSIBILITY, DATE, LOCATION, METRIC, OTHER
    fact_value TEXT NOT NULL,
    source VARCHAR(100) NOT NULL, -- PROFILE, MASTER_RESUME, EXPERIENCE, PROJECT, SKILL, EDUCATION, MANUAL
    source_reference VARCHAR(255),
    evidence_level VARCHAR(50) NOT NULL DEFAULT 'VERIFIED', -- VERIFIED, SUPPORTED, UNKNOWN
    verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidate_facts_profile ON candidate_facts(candidate_profile_id);
CREATE INDEX IF NOT EXISTS idx_candidate_facts_category ON candidate_facts(category);
CREATE INDEX IF NOT EXISTS idx_candidate_facts_evidence ON candidate_facts(evidence_level);

CREATE TABLE IF NOT EXISTS resume_tailoring_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    tailored_resume_id UUID REFERENCES tailored_resumes(id) ON DELETE SET NULL,
    source_resume_version INT NOT NULL DEFAULT 1,
    validator_version VARCHAR(50) NOT NULL DEFAULT 'v2.0-claim-auditor',
    claims_checked INT NOT NULL DEFAULT 0,
    claims_passed INT NOT NULL DEFAULT 0,
    claims_failed INT NOT NULL DEFAULT 0,
    validation_status VARCHAR(50) NOT NULL, -- READY_FOR_DOWNLOAD, VALIDATION_FAILED, BLOCKED
    failure_reason TEXT,
    audit_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tailoring_audits_candidate ON resume_tailoring_audits(candidate_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_audits_job ON resume_tailoring_audits(job_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_audits_status ON resume_tailoring_audits(validation_status);
