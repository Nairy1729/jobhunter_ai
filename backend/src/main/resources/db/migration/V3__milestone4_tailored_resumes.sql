-- JobHunter AI Schema Migration V3
-- Milestone 4: Intelligent Resume Tailoring, Application Packages, LaTeX/PDF Generation & Validation

CREATE TABLE IF NOT EXISTS tailored_resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    master_resume_id UUID REFERENCES resumes(id) ON DELETE SET NULL,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    version_number INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'GENERATED', 
    -- DRAFT, GENERATED, USER_REVIEW, APPROVED, EXPORTED
    target_role VARCHAR(255) NOT NULL,
    target_company VARCHAR(255) NOT NULL,
    tailoring_plan JSONB NOT NULL DEFAULT '{}'::jsonb,
    tailored_markdown TEXT NOT NULL,
    latex_source TEXT,
    pdf_file_path VARCHAR(500),
    pdf_file_size_bytes BIGINT,
    ats_score_estimate NUMERIC(5,2),
    validation_report JSONB NOT NULL DEFAULT '{}'::jsonb,
    generation_prompt_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tailored_resumes_profile ON tailored_resumes(candidate_profile_id);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_job ON tailored_resumes(job_id);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_status ON tailored_resumes(status);

ALTER TABLE applications 
    ADD COLUMN IF NOT EXISTS tailored_resume_id UUID REFERENCES tailored_resumes(id) ON DELETE SET NULL;
