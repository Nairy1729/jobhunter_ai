-- V5__milestone5_intelligent_ranking.sql
-- Milestone 5: Intelligent Job Ranking, Prioritization & Discovery Quality

-- 1. Add prioritization and freshness columns to job_matches
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS priority_category VARCHAR(50);
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS freshness VARCHAR(20);
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS days_since_posted INT;
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS why_this_job JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS potential_concerns JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE job_matches ADD COLUMN IF NOT EXISTS requirement_coverage JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 2. Add performance indices
CREATE INDEX IF NOT EXISTS idx_job_matches_prio_cat ON job_matches(priority_category);
CREATE INDEX IF NOT EXISTS idx_job_matches_freshness ON job_matches(freshness);
CREATE INDEX IF NOT EXISTS idx_jobs_posting_date ON jobs(posting_date DESC NULLS LAST);
