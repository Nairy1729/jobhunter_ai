-- JobHunter AI Schema Migration V2
-- Milestone 3: Semantic Understanding, Candidate Knowledge Model, Vector Store & Evidence Grounding

-- 1. pgvector Extension Check
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;
        RAISE NOTICE 'pgvector extension successfully activated.';
    ELSE
        RAISE NOTICE 'pgvector extension is not available in pg_available_extensions. Falling back to universal JSONB vector storage.';
    END IF;
END $$;

-- 2. Professional Experiences for Candidate Knowledge Representation
CREATE TABLE IF NOT EXISTS candidate_experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    duration VARCHAR(100),
    technologies JSONB NOT NULL DEFAULT '[]'::jsonb,
    responsibilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    achievements JSONB NOT NULL DEFAULT '[]'::jsonb,
    domain VARCHAR(150),
    evidence_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cand_exp_profile ON candidate_experiences(candidate_profile_id);

-- 3. Projects for Candidate Knowledge Representation
CREATE TABLE IF NOT EXISTS candidate_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    technologies JSONB NOT NULL DEFAULT '[]'::jsonb,
    architecture TEXT,
    responsibilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    measurable_outcomes JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence_text TEXT,
    project_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cand_proj_profile ON candidate_projects(candidate_profile_id);

-- 4. Extend Candidate Skills with Commercial vs Project Classification
ALTER TABLE candidate_skills
    ADD COLUMN IF NOT EXISTS experience_type VARCHAR(50) NOT NULL DEFAULT 'COMMERCIAL',
    ADD COLUMN IF NOT EXISTS confidence NUMERIC(3,2) NOT NULL DEFAULT 1.00,
    ADD COLUMN IF NOT EXISTS evidence_source VARCHAR(255);

-- 5. Extend Job Requirements with Implied and Raw Snippet Support
ALTER TABLE job_requirements
    ADD COLUMN IF NOT EXISTS is_implied BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS raw_text_snippet TEXT;

-- 6. Universal Embedding Storage for Jobs
CREATE TABLE IF NOT EXISTS job_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    embedding_model VARCHAR(100) NOT NULL,
    dimensions INT NOT NULL DEFAULT 768,
    embedding_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_embeddings_job_model UNIQUE (job_id, embedding_model)
);
CREATE INDEX IF NOT EXISTS idx_job_embeddings_job ON job_embeddings(job_id);

-- 7. Universal Embedding Storage for Candidate Profiles
CREATE TABLE IF NOT EXISTS candidate_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    embedding_model VARCHAR(100) NOT NULL,
    dimensions INT NOT NULL DEFAULT 768,
    embedding_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_embeddings_model UNIQUE (candidate_profile_id, embedding_model)
);
CREATE INDEX IF NOT EXISTS idx_cand_embeddings_profile ON candidate_embeddings(candidate_profile_id);

-- 8. Conditionally add native vector columns if pgvector is active
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'job_embeddings' AND column_name = 'embedding_vector') THEN
            ALTER TABLE job_embeddings ADD COLUMN embedding_vector vector(768);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'candidate_embeddings' AND column_name = 'embedding_vector') THEN
            ALTER TABLE candidate_embeddings ADD COLUMN embedding_vector vector(768);
        END IF;
        RAISE NOTICE 'Native vector(768) columns successfully provisioned.';
    END IF;
END $$;
