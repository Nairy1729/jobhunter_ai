-- JobHunter AI Schema Migration V1
-- Comprehensive DDL for Candidate Profiles, Jobs, Matching, Applications, and Intelligence

-- 1. Authentication & Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CANDIDATE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email ON users(email);

-- 2. Candidate Profiles
CREATE TABLE candidate_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    headline VARCHAR(255) NOT NULL,
    summary TEXT,
    years_of_experience NUMERIC(4,1) NOT NULL DEFAULT 0.0,
    current_location VARCHAR(150),
    preferred_locations JSONB NOT NULL DEFAULT '[]'::jsonb,
    work_modes JSONB NOT NULL DEFAULT '[]'::jsonb,
    min_salary_inr NUMERIC(12,2) DEFAULT 1000000.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    target_roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    github_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    portfolio_url VARCHAR(255),
    phone_number VARCHAR(50),
    raw_profile_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_profiles_user UNIQUE (user_id)
);

-- 3. Skills Taxonomy
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    aliases JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_category ON skills(category);

CREATE TABLE candidate_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE RESTRICT,
    proficiency_level VARCHAR(50) NOT NULL DEFAULT 'INTERMEDIATE',
    years_experience NUMERIC(3,1),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_skill UNIQUE (candidate_profile_id, skill_id)
);

-- 4. Resumes
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    raw_extracted_text TEXT NOT NULL,
    structured_content JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_master BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resume_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE RESTRICT,
    version_label VARCHAR(150) NOT NULL,
    tailored_markdown TEXT NOT NULL,
    rendered_file_path VARCHAR(500),
    diff_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    generation_prompt_hash VARCHAR(64),
    ats_score_estimate NUMERIC(5,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_resume_versions_master ON resume_versions(master_resume_id);

-- 5. Companies & Discovery Sources
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    domain VARCHAR(255),
    career_page_url VARCHAR(500),
    ats_provider VARCHAR(50),
    industry VARCHAR(100),
    company_size VARCHAR(50),
    headquarters VARCHAR(150),
    known_tech_stack JSONB NOT NULL DEFAULT '[]'::jsonb,
    intelligence_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_companies_name ON companies(name);

CREATE TABLE job_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    source_type VARCHAR(50) NOT NULL,
    base_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute INT NOT NULL DEFAULT 10,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE search_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_source_id UUID NOT NULL REFERENCES job_sources(id),
    query_string TEXT NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    jobs_discovered_count INT NOT NULL DEFAULT 0,
    jobs_ingested_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

-- 6. Jobs & Requirements
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    job_source_id UUID NOT NULL REFERENCES job_sources(id) ON DELETE RESTRICT,
    search_run_id UUID REFERENCES search_runs(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    normalized_title VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    location VARCHAR(200) NOT NULL,
    work_mode VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    employment_type VARCHAR(50) DEFAULT 'FULL_TIME',
    min_experience_years NUMERIC(3,1),
    max_experience_years NUMERIC(3,1),
    min_salary NUMERIC(12,2),
    max_salary NUMERIC(12,2),
    salary_currency VARCHAR(10),
    job_url VARCHAR(1000) NOT NULL,
    canonical_url VARCHAR(1000) NOT NULL,
    canonical_url_hash VARCHAR(64) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    raw_description_markdown TEXT NOT NULL,
    structured_job_spec JSONB NOT NULL DEFAULT '{}'::jsonb,
    posting_date DATE,
    deadline_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    pipeline_status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_jobs_canonical_hash UNIQUE (canonical_url_hash),
    CONSTRAINT uk_jobs_content_hash UNIQUE (content_hash)
);
CREATE INDEX idx_jobs_company ON jobs(company_id);
CREATE INDEX idx_jobs_status ON jobs(pipeline_status);
CREATE INDEX idx_jobs_work_mode ON jobs(work_mode);
CREATE INDEX idx_jobs_posting_date ON jobs(posting_date DESC);

CREATE TABLE job_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    requirement_type VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    skill_id UUID REFERENCES skills(id) ON DELETE SET NULL,
    inferred_importance NUMERIC(3,2) NOT NULL DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_job_reqs_job ON job_requirements(job_id);

-- 7. Matching & Edge System
CREATE TABLE job_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    recommendation VARCHAR(50) NOT NULL,
    priority_score NUMERIC(5,2) NOT NULL DEFAULT 0.0,
    queue_tier VARCHAR(50) NOT NULL DEFAULT 'REVIEW',
    strong_matches JSONB NOT NULL DEFAULT '[]'::jsonb,
    partial_matches JSONB NOT NULL DEFAULT '[]'::jsonb,
    gaps JSONB NOT NULL DEFAULT '[]'::jsonb,
    transferable_experience JSONB NOT NULL DEFAULT '[]'::jsonb,
    risk_factors JSONB NOT NULL DEFAULT '[]'::jsonb,
    advantage_report JSONB NOT NULL DEFAULT '{}'::jsonb,
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_match UNIQUE (job_id, candidate_profile_id)
);
CREATE INDEX idx_job_matches_queue ON job_matches(queue_tier, priority_score DESC);
CREATE INDEX idx_job_matches_rec ON job_matches(recommendation);

-- 8. Applications & Gate
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE RESTRICT,
    resume_version_id UUID REFERENCES resume_versions(id) ON DELETE SET NULL,
    current_status VARCHAR(50) NOT NULL DEFAULT 'SHORTLISTED',
    applied_at TIMESTAMPTZ,
    application_portal_url VARCHAR(1000),
    human_approved BOOLEAN NOT NULL DEFAULT FALSE,
    human_approved_at TIMESTAMPTZ,
    submission_proof_type VARCHAR(50),
    submission_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_candidate_job UNIQUE (candidate_profile_id, job_id)
);
CREATE INDEX idx_applications_status ON applications(current_status);

CREATE TABLE application_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    question_type VARCHAR(50) NOT NULL,
    question_prompt TEXT NOT NULL,
    generated_answer TEXT NOT NULL,
    user_edited_answer TEXT,
    grounding_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_app_answers_app ON application_answers(application_id);

CREATE TABLE application_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    user_edited_content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    transitioned_by VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_app_history_app ON application_status_history(application_id);

-- 9. Interview Preparation
CREATE TABLE interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    round_type VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    interviewer_names VARCHAR(255),
    interviewer_titles VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_interviews_app ON interviews(application_id);

CREATE TABLE interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID REFERENCES interviews(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    context_rationale TEXT,
    suggested_answer_points TEXT,
    is_mastered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_interview_q_job ON interview_questions(job_id);

CREATE TABLE interview_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    questions_asked TEXT,
    topics_covered TEXT,
    candidate_strengths_noted TEXT,
    candidate_gaps_noted TEXT,
    outcome VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Audit & Agent Runs
CREATE TABLE agent_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_name VARCHAR(100) NOT NULL,
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    llm_provider VARCHAR(50),
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd NUMERIC(8,5) NOT NULL DEFAULT 0.0,
    execution_time_ms BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_agent_runs_name ON agent_runs(agent_name);

-- 11. Initial Skills Taxonomy Seed
INSERT INTO skills (name, category, aliases) VALUES
('Java', 'LANGUAGE', '["java 17", "java 11", "core java", "jdk"]'::jsonb),
('Spring Boot', 'FRAMEWORK', '["springboot", "spring-boot", "spring framework"]'::jsonb),
('Spring Security', 'SECURITY', '["spring-security", "oauth2", "jwt auth"]'::jsonb),
('PostgreSQL', 'DATABASE', '["postgres", "psql", "postgresql 16"]'::jsonb),
('SQL', 'DATABASE', '["relational database", "rdbms", "sql queries"]'::jsonb),
('React', 'FRAMEWORK', '["react.js", "reactjs", "react 18"]'::jsonb),
('Node.js', 'RUNTIME', '["nodejs", "node"]'::jsonb),
('TypeScript', 'LANGUAGE', '["ts"]'::jsonb),
('JavaScript', 'LANGUAGE', '["js", "es6"]'::jsonb),
('REST APIs', 'ARCHITECTURE', '["restful apis", "rest api", "api design"]'::jsonb),
('JWT', 'SECURITY', '["json web tokens", "jwt authentication"]'::jsonb),
('C#', 'LANGUAGE', '["csharp", "c#.net"]'::jsonb),
('.NET Core', 'FRAMEWORK', '["asp.net core", ".net 6", ".net 8", "dotnet"]'::jsonb),
('Docker', 'DEVOPS', '["docker compose", "containerization"]'::jsonb),
('Git', 'TOOL', '["git version control", "github", "gitlab"]'::jsonb),
('MERN Stack', 'CONCEPT', '["mern", "mongodb express react node"]'::jsonb),
('Enterprise Architecture', 'CONCEPT', '["enterprise applications", "clean architecture", "tiered architecture"]'::jsonb);

-- 12. Default Discovery Job Sources
INSERT INTO job_sources (name, source_type, base_url, rate_limit_per_minute) VALUES
('FIRECRAWL_GREENHOUSE', 'FIRECRAWL_SEARCH', 'https://boards.greenhouse.io', 12),
('FIRECRAWL_LEVER', 'FIRECRAWL_SEARCH', 'https://jobs.lever.co', 12),
('FIRECRAWL_WORKDAY', 'FIRECRAWL_SEARCH', 'https://myworkdayjobs.com', 8),
('FIRECRAWL_ASHBY', 'FIRECRAWL_SEARCH', 'https://jobs.ashbyhq.com', 12);
