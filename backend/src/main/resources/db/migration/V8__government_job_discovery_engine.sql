-- JobHunter AI Schema Migration V8
-- Dedicated Government Job Discovery & Verification Engine

-- 1. Government Source Registry
CREATE TABLE IF NOT EXISTS government_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    official_domain VARCHAR(255) NOT NULL,
    state VARCHAR(100),
    district VARCHAR(100),
    source_type VARCHAR(60) NOT NULL,
    department VARCHAR(255),
    authority VARCHAR(255),
    priority VARCHAR(40) NOT NULL DEFAULT 'TIER_1_OFFICIAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    crawl_frequency VARCHAR(40) NOT NULL DEFAULT 'DAILY',
    last_checked TIMESTAMPTZ,
    last_successful_crawl TIMESTAMPTZ,
    last_attempt TIMESTAMPTZ,
    failure_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gov_sources_type ON government_sources(source_type);
CREATE INDEX IF NOT EXISTS idx_gov_sources_state ON government_sources(state);
CREATE INDEX IF NOT EXISTS idx_gov_sources_priority ON government_sources(priority);
CREATE INDEX IF NOT EXISTS idx_gov_sources_active ON government_sources(active);

-- 2. Government Jobs Canonical Records
CREATE TABLE IF NOT EXISTS government_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    state VARCHAR(100),
    district VARCHAR(100),
    block VARCHAR(100),
    employment_type VARCHAR(60) NOT NULL DEFAULT 'REGULAR',
    vacancies_count INT,
    vacancies_breakdown_json TEXT,
    salary VARCHAR(255),
    salary_min NUMERIC(12,2),
    salary_max NUMERIC(12,2),
    pay_level VARCHAR(100),
    honorarium BOOLEAN NOT NULL DEFAULT FALSE,
    application_mode VARCHAR(60) NOT NULL DEFAULT 'ONLINE',
    application_fee VARCHAR(255),
    application_start_date TIMESTAMPTZ,
    application_last_date TIMESTAMPTZ,
    exam_date TIMESTAMPTZ,
    interview_date TIMESTAMPTZ,
    source_url VARCHAR(1000),
    notification_url VARCHAR(1000),
    application_url VARCHAR(1000),
    authority VARCHAR(255),
    source_domain VARCHAR(255),
    notification_number VARCHAR(255),
    verification_status VARCHAR(60) NOT NULL DEFAULT 'VERIFIED_OFFICIAL',
    authenticity_score NUMERIC(5,2) NOT NULL DEFAULT 95.00,
    authenticity_level VARCHAR(40) NOT NULL DEFAULT 'VERIFIED',
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    raw_content TEXT,
    source_id UUID REFERENCES government_sources(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gov_jobs_state_district ON government_jobs(state, district);
CREATE INDEX IF NOT EXISTS idx_gov_jobs_employment_type ON government_jobs(employment_type);
CREATE INDEX IF NOT EXISTS idx_gov_jobs_status ON government_jobs(status);
CREATE INDEX IF NOT EXISTS idx_gov_jobs_verification_status ON government_jobs(verification_status);
CREATE INDEX IF NOT EXISTS idx_gov_jobs_last_date ON government_jobs(application_last_date);

-- 3. Government Job Eligibility Criteria
CREATE TABLE IF NOT EXISTS government_job_eligibility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES government_jobs(id) ON DELETE CASCADE,
    gender VARCHAR(40) NOT NULL DEFAULT 'NOT_SPECIFIED',
    minimum_age INT,
    maximum_age INT,
    education_json TEXT NOT NULL DEFAULT '[]',
    experience_json TEXT NOT NULL DEFAULT '[]',
    experience_years_min NUMERIC(4,1) DEFAULT 0.0,
    domicile VARCHAR(255) NOT NULL DEFAULT 'NOT_SPECIFIED',
    category_reservations_json TEXT NOT NULL DEFAULT '[]',
    pwd_eligible BOOLEAN DEFAULT TRUE,
    ex_serviceman_eligible BOOLEAN DEFAULT TRUE,
    other_conditions TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_gov_job_eligibility UNIQUE (job_id)
);

CREATE INDEX IF NOT EXISTS idx_gov_job_elig_gender ON government_job_eligibility(gender);

-- 4. Government Job Field Evidence (Transparency & Zero Fabrication)
CREATE TABLE IF NOT EXISTS government_job_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES government_jobs(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    field_value TEXT,
    source_document VARCHAR(500),
    page_or_section VARCHAR(100),
    excerpt TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gov_evidence_job ON government_job_evidence(job_id);

-- 5. Government Job Corrigenda & Extensions
CREATE TABLE IF NOT EXISTS government_job_corrigenda (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES government_jobs(id) ON DELETE CASCADE,
    notice_type VARCHAR(60) NOT NULL DEFAULT 'CORRIGENDUM',
    title VARCHAR(500) NOT NULL,
    document_url VARCHAR(1000),
    issue_date TIMESTAMPTZ,
    description TEXT,
    revised_last_date TIMESTAMPTZ,
    revised_vacancies INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gov_corrigenda_job ON government_job_corrigenda(job_id);

-- 6. Dedicated Candidate Government Profile
CREATE TABLE IF NOT EXISTS candidate_government_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    age INT,
    dob DATE,
    gender VARCHAR(40),
    state VARCHAR(100),
    district VARCHAR(100),
    domicile_state VARCHAR(100),
    domicile_district VARCHAR(100),
    highest_education VARCHAR(100),
    degrees_json TEXT NOT NULL DEFAULT '[]',
    passing_year INT,
    years_of_experience NUMERIC(4,1) NOT NULL DEFAULT 0.0,
    category VARCHAR(50) NOT NULL DEFAULT 'UR/GEN',
    pwd BOOLEAN NOT NULL DEFAULT FALSE,
    ex_serviceman BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_states_json TEXT NOT NULL DEFAULT '[]',
    preferred_districts_json TEXT NOT NULL DEFAULT '[]',
    preferred_employment_types_json TEXT NOT NULL DEFAULT '[]',
    skills_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cand_gov_profile_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_cand_gov_profile_user ON candidate_government_profiles(user_id);
