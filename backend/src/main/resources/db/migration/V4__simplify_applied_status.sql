-- V4__simplify_applied_status.sql
-- JobHunter AI Final Scope Correction:
-- Remove obsolete application tracking, interview, and outcome analytics tables.
-- Simplify application state to: applied (boolean) + applied_at (timestamp).

-- 1. Drop unused interview and lifecycle tables
DROP TABLE IF EXISTS interview_feedback CASCADE;
DROP TABLE IF EXISTS interview_questions CASCADE;
DROP TABLE IF EXISTS interviews CASCADE;
DROP TABLE IF EXISTS application_status_history CASCADE;
DROP TABLE IF EXISTS application_documents CASCADE;
DROP TABLE IF EXISTS application_answers CASCADE;

-- 2. Drop obsolete lifecycle columns from applications
ALTER TABLE applications DROP COLUMN IF EXISTS current_status CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS application_portal_url CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS human_approved CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS human_approved_at CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS submission_proof_type CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS submission_notes CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS resume_version_id CASCADE;
ALTER TABLE applications DROP COLUMN IF EXISTS tailored_resume_id CASCADE;

-- 3. Drop circular reference from tailored_resumes if present
ALTER TABLE tailored_resumes DROP COLUMN IF EXISTS application_id CASCADE;

-- 4. Add simplified applied boolean and ensure index
ALTER TABLE applications ADD COLUMN IF NOT EXISTS applied BOOLEAN NOT NULL DEFAULT TRUE;

DROP INDEX IF EXISTS idx_applications_status;
CREATE INDEX IF NOT EXISTS idx_applications_cand_job ON applications(candidate_profile_id, job_id);
CREATE INDEX IF NOT EXISTS idx_applications_applied ON applications(candidate_profile_id, applied);
