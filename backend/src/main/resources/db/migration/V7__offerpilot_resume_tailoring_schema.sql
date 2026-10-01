-- JobHunter AI Schema Migration V7
-- OfferPilot AI Resume Tailoring Engine Schema Extension

ALTER TABLE tailored_resumes
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS target_job_title VARCHAR(150),
    ADD COLUMN IF NOT EXISTS template_name VARCHAR(80) NOT NULL DEFAULT 'PROFESSIONAL_DEFAULT',
    ADD COLUMN IF NOT EXISTS structured_content_json TEXT,
    ADD COLUMN IF NOT EXISTS matched_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS partially_matched_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS missing_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS tailoring_notes_json TEXT,
    ADD COLUMN IF NOT EXISTS latex_file_path VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_tailored_resumes_display_name ON tailored_resumes(display_name);
