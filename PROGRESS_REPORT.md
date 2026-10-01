# JobHunter AI — Engineering Progress & Implementation Report

**Project**: JobHunter AI  
**Author**: Lead Engineer & AI Systems Architect  
**Date**: September 29, 2026  
**Status**: Phase 0 (Discovery & Architecture) & Milestone 1 (Foundation & Profile Engine) Completed  
**Active Services**:
* **Backend API**: `http://localhost:8085` (Spring Boot 3.3.4, Java 17 LTS)
* **Frontend Web App**: `http://localhost:5173` (React 18.3, TypeScript, Vite, Tailwind CSS)
* **Database**: PostgreSQL 16.2 (`jobhunter_db` on port 5432, managed via Flyway)

---

## 1. Executive Summary

JobHunter AI is an autonomous, high-conviction job discovery, application preparation, and interview readiness platform for software engineers. It departs fundamentally from high-volume, low-quality scrapers by emphasizing:
1. **Semantic Understanding**: Mapping candidate experience to job requirements rather than shallow keyword matching.
2. **The "Edge" System**: Generating an 11-dimension Application Advantage Report and grounded, tailored resume versions with side-by-side visual diffs.
3. **Absolute Factual Truthfulness**: Enforcing an automated Grounding Verification Gate that rejects any fabricated claims, non-existent skills, or synthetic metrics.
4. **Mandatory Human Approval Gate**: Providing zero technical pathway for silent, autonomous background job submissions.
5. **Continuous Learning**: Correlating application and interview outcomes to refine search queries and priority ranking.

To date, **Phase 0 (Discovery & Architecture)** and **Milestone 1 (Core Foundation & Candidate Profile Engine)** are 100% complete and verified live on system.

---

## 2. Phase 0: System Architecture & Documentation

Seven foundational engineering specifications were authored and committed to [`docs/`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs):

| Document | File Path | Core Scope & Architectural Decisions |
| :--- | :--- | :--- |
| **Product Specification** | [`docs/PRODUCT_SPEC.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/PRODUCT_SPEC.md) | Defines the 10-stage pipeline (Discover → Filter → Understand → Match → Identify Gaps → Tailor → Prepare → Human Review → Apply → Track/Learn), candidate profile baseline, The Edge system, priority queue tiers, and KPIs. |
| **System Architecture** | [`docs/ARCHITECTURE.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/ARCHITECTURE.md) | C4 Level 1 & 2 topology, Spring Boot 3 + React 18 stack, decoupled `LlmProvider` SPI (Gemini/Claude/OpenAI/Ollama), cost governor, and Resilience4j patterns. |
| **Database Design** | [`docs/DATABASE_DESIGN.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/DATABASE_DESIGN.md) | 3NF PostgreSQL 16 schema, Flyway migrations, ER diagram, JSONB columns for flexible data, GIN full-text search indexes, and two-tier deduplication hashes. |
| **Agent Architecture** | [`docs/AGENT_ARCHITECTURE.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/AGENT_ARCHITECTURE.md) | Multi-agent specialist architecture (Discovery, Extraction, Matching, Advantage, Tailoring, Q&A, Assistant, Interview Prep, Learning), deterministic vs AI division of labor, schema contracts, and anti-hallucination verification. |
| **MVP Implementation Plan** | [`docs/MVP_PLAN.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/MVP_PLAN.md) | 6-milestone delivery roadmap for the 14 core MVP capabilities, Definition of Done (DoD) per milestone, and testing strategy. |
| **Security Model** | [`docs/SECURITY_MODEL.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/SECURITY_MODEL.md) | STRIDE threat matrix, stateless JWT + BCrypt auth, human-in-the-loop security lock, prompt injection defenses, SSRF prevention, and secret management. |
| **Firecrawl Integration** | [`docs/FIRECRAWL_INTEGRATION.md`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/docs/FIRECRAWL_INTEGRATION.md) | Firecrawl API v1 integration (`/v1/search`, `/v1/scrape`, `/v1/crawl`, `/v1/map`), source adapter pattern (Greenhouse, Lever, Workday), SHA-256 deduplication, and rate limiting. |

---

## 3. Milestone 1: Implementation Details

### 3.1 PostgreSQL 16 Database & Flyway Migrations
* Database instance configured at `localhost:5432` with database `jobhunter_db` and dedicated user `jobhunter`.
* Schema migration [`V1__init_schema.sql`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/resources/db/migration/V1__init_schema.sql) applied successfully:
  * **Core Entities (19 tables)**: `users`, `candidate_profiles`, `skills`, `candidate_skills`, `resumes`, `resume_versions`, `companies`, `job_sources`, `search_runs`, `jobs`, `job_requirements`, `job_matches`, `applications`, `application_answers`, `application_documents`, `application_status_history`, `interviews`, `interview_questions`, `interview_feedback`, `agent_runs`.
  * **Optimized Indexes**: Unique SHA-256 constraints (`uk_jobs_canonical_hash`, `uk_jobs_content_hash`), composite queue index (`queue_tier, priority_score DESC`), and skills taxonomy lookup indexes.
  * **JSONB Capabilities**: Native PostgreSQL `jsonb` configured with JDBC parameter auto-casting (`stringtype=unspecified`).

### 3.2 Spring Boot 3.3.4 Backend (`backend/`)
* **Java 17 LTS Core**: Clean, modular structure using Spring Boot Maven Wrapper (`./mvnw.cmd`).
* **Port Allocation**: Configured to port `8085` via [`application.yml`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/resources/application.yml) to preserve existing processes on port 8080.
* **Spring Security 6 & Stateless JWT**:
  * JJWT 0.12.6 implementation ([`JwtTokenProvider.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/security/JwtTokenProvider.java)) using HMAC-SHA256 with 15-minute access tokens and 7-day refresh tokens.
  * Once-per-request filter ([`JwtAuthenticationFilter.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/security/JwtAuthenticationFilter.java)) and BCrypt password encryption (strength 12).
* **Data Seeding Service**:
  * [`DataSeederService.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/service/DataSeederService.java) automatically initializes the candidate user (`candidate@jobhunter.ai` / `password123`) and profile on startup:
    * **Headline**: Software Engineer / Backend & Full Stack Engineer
    * **Years of Experience**: 2.5 years commercial experience
    * **Target Compensation**: ₹10,00,000 (10+ LPA baseline, fully customizable)
    * **Target Roles**: Software Engineer, Associate Software Engineer, Backend Engineer, Full Stack Engineer, Java Developer, Spring Boot Developer
    * **Preferred Locations & Modes**: Bangalore, Hyderabad, Pune, Remote, International (Remote, Hybrid, Relocation)
    * **15 Seeded Technical Skills with Verified Evidence**:
      * *Core / Primary*: Java (Advanced, 2.5y), Spring Boot (Advanced, 2.5y), Spring Security (Intermediate, 2.0y), PostgreSQL (Advanced, 2.5y), SQL (Advanced, 2.5y), REST APIs (Advanced, 2.5y), JWT (Advanced, 2.0y), React (Intermediate, 1.5y), Node.js (Intermediate, 1.5y), Git (Advanced, 2.5y), Enterprise Architecture (Intermediate, 2.0y)
      * *Secondary*: MERN Stack (Intermediate, 1.5y), C# (Intermediate, 1.0y), .NET Core (Intermediate, 1.0y), Docker (Intermediate, 1.0y)
* **Document Processing & Resume Ingestion**:
  * [`ResumeParserService.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/service/ResumeParserService.java) using **Apache PDFBox 3.0.3**:
  * Extracts text from uploaded PDF resumes, computes character count, detects matching skills against the database taxonomy, stores the document in `./storage/resumes`, and creates an immutable master resume record.
* **REST Controllers**:
  * [`AuthController.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/controller/AuthController.java): `/api/auth/register`, `/api/auth/login`, `/api/auth/me`
  * [`CandidateProfileController.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/controller/CandidateProfileController.java): `GET /api/profile`, `PUT /api/profile`, `PUT /api/profile/skills`
  * [`ResumeController.java`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/controller/ResumeController.java): `POST /api/resumes/upload`, `GET /api/resumes`, `GET /api/resumes/{id}`

### 3.3 Modern React 18 Frontend (`frontend/`)
* **Framework**: React 18.3 + TypeScript 5.6 + Vite 5.4 + Tailwind CSS.
* **Components**:
  * [`Navbar.tsx`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/frontend/src/components/Navbar.tsx): Application branding, copilot badge, milestone navigation tabs, user identification, and sign-out.
  * [`CandidateProfileEditor.tsx`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/frontend/src/components/CandidateProfileEditor.tsx): Complete UI for viewing and editing headline, summary, commercial YOE, base location, target CTC (INR), currency, preferred locations, target roles, work modes, and professional links.
  * **Verified Technical Skills Matrix**: Table displaying proficiency badges (`ADVANCED`, `INTERMEDIATE`, etc.), verified commercial duration, core vs. secondary indicator, truthful project evidence, and inline skill addition form.
  * [`ResumeUploadModal.tsx`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/frontend/src/components/ResumeUploadModal.tsx): Interactive PDF upload modal with size validation, extraction progress spinner, and detected skill preview.
  * [`api/client.ts`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/frontend/src/api/client.ts): Axios client configured with JWT bearer authentication interceptors and typed API wrappers.
  * [`vite.config.ts`](file:///C:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/frontend/vite.config.ts): Configured with a reverse proxy forwarding `/api` calls directly to `http://localhost:8085`.

---

## 4. Live Verification & Test Results

### 4.1 Automated Backend Test Suite
```
[INFO] Running com.jobhunter.JobHunterAiApplicationTests
2026-09-29T21:16:11.241 INFO org.flywaydb.core.FlywayExecutor : Database: jdbc:postgresql://localhost:5432/jobhunter_db (PostgreSQL 16.2)
2026-09-29T21:16:11.372 INFO o.f.core.internal.command.DbMigrate : Current version of schema "public": 1
2026-09-29T21:16:11.377 INFO o.f.core.internal.command.DbMigrate : Schema "public" is up to date. No migration necessary.
2026-09-29T21:16:14.061 INFO j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory
2026-09-29T21:16:18.004 INFO c.jobhunter.service.DataSeederService : Seeding candidate profile for: candidate@jobhunter.ai
2026-09-29T21:16:18.560 INFO c.jobhunter.service.DataSeederService : Candidate profile seeding completed successfully!
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4.2 Database Queries (PostgreSQL 16)
```sql
SELECT headline, years_of_experience, min_salary_inr, count(cs.id) as skill_count 
FROM candidate_profiles p 
LEFT JOIN candidate_skills cs ON p.id = cs.candidate_profile_id 
GROUP BY p.headline, p.years_of_experience, p.min_salary_inr;
```
* **Result**:
  * Headline: `Software Engineer / Backend & Full Stack Engineer`
  * Experience: `2.5 years`
  * Target CTC: `₹10,00,000.00`
  * Seeded Verified Skills: `15`

### 4.3 HTTP REST API Verification
* `POST http://localhost:8085/api/auth/login` → **200 OK** (Returned valid JWT token for `candidate@jobhunter.ai`).
* `GET http://localhost:8085/api/profile` → **200 OK** (Retrieved candidate profile, preferred locations, target roles, and 15 skills).
* `PUT http://localhost:8085/api/profile` → **200 OK** (Verified real-time update of headline and preferences).

### 4.4 Frontend Production & Development Verification
* Production build: `npm run build` completed with zero TypeScript errors; generated optimized bundle (`dist/index.html`, `dist/assets/index-*.js`, `dist/assets/index-*.css`).
* Development server: Running on `http://localhost:5173/`, returning HTTP 200 and hot module replacement.

---

## 5. Repository File Structure

```
jobhunter-ai/
├── docs/
│   ├── PRODUCT_SPEC.md            # Product vision, 10-stage pipeline, candidate profile
│   ├── ARCHITECTURE.md            # C4 topology, tech stack rationale, data flows
│   ├── DATABASE_DESIGN.md         # 3NF schema, 19 tables, JSONB, GIN indexes, Flyway
│   ├── AGENT_ARCHITECTURE.md      # Multi-agent contracts, anti-hallucination gate
│   ├── MVP_PLAN.md                # 6-milestone roadmap, DoD, test strategy
│   ├── SECURITY_MODEL.md          # STRIDE threat model, JWT auth, human gate
│   └── FIRECRAWL_INTEGRATION.md   # Firecrawl endpoints, source adapters, deduplication
│
├── backend/
│   ├── pom.xml                    # Spring Boot 3.3.4, PostgreSQL, Flyway, JJWT, PDFBox
│   ├── mvnw / mvnw.cmd / .mvn     # Maven Wrapper (v3.9.16)
│   └── src/
│       ├── main/
│       │   ├── java/com/jobhunter/
│       │   │   ├── JobHunterAiApplication.java
│       │   │   ├── config/        # SecurityConfig, CorsConfig
│       │   │   ├── controller/    # AuthController, CandidateProfileController, ResumeController
│       │   │   ├── model/
│       │   │   │   ├── dto/       # AuthRequest, AuthResponse, CandidateProfileDto, etc.
│       │   │   │   └── entity/    # User, CandidateProfile, Skill, CandidateSkill, Resume, Job, etc.
│       │   │   ├── repository/    # 10 Spring Data JPA repositories
│       │   │   ├── security/      # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetails
│       │   │   └── service/       # AuthService, CandidateProfileService, ResumeParserService, DataSeederService
│       │   └── resources/
│       │       ├── application.yml # Port 8085, DB credentials, Flyway, JWT configs
│       │       └── db/migration/
│       │           └── V1__init_schema.sql # 19-table DDL and initial seeds
│       └── test/                  # JUnit 5 & Spring Boot test suite
│
├── frontend/
│   ├── package.json               # React 18, Vite, TypeScript, Tailwind CSS, TanStack Query
│   ├── vite.config.ts             # Port 5173 with proxy to backend port 8085
│   ├── tailwind.config.js
│   ├── index.html
│   └── src/
│       ├── App.tsx                # Main application state & auth flow
│       ├── main.tsx
│       ├── index.css
│       ├── types/index.ts         # TypeScript definitions matching backend DTOs
│       ├── api/client.ts          # Axios client with JWT interceptor
│       └── components/
│           ├── Navbar.tsx
│           ├── CandidateProfileEditor.tsx
│           └── ResumeUploadModal.tsx
```

---

## 6. Next Steps: Milestone 2 Roadmap

With Milestone 1 operating cleanly, the verified candidate profile is in place to power the discovery pipeline.

**Milestone 2 Objectives (Firecrawl Web Intelligence & Deduplication)**:
1. **Firecrawl Service Adapter**:
   * Create `FirecrawlClient` utilizing Resilience4j (Rate Limiter, Circuit Breaker, Exponential Backoff).
   * Implement calls to `/v1/search` for fresh job openings and `/v1/scrape` for clean markdown extraction.
2. **Precision Search Strategy**:
   * Build query generator focusing on ATS domains and candidate skills:
     * `site:boards.greenhouse.io ("Spring Boot" OR "Java") ("PostgreSQL" OR "SQL") ("India" OR "Remote")`
     * `site:jobs.lever.co ("Backend Engineer" OR "Software Engineer") ("Java" OR "React")`
3. **Pluggable Source Adapters**:
   * Implement `GreenhouseAdapter`, `LeverAdapter`, `WorkdayAdapter`, and `GenericCareerAdapter` to normalize URLs and strip tracking parameters (`gh_src`, etc.).
4. **Two-Tier Deduplication Engine**:
   * *Tier 1*: Normalized canonical URL hash check prior to scraping.
   * *Tier 2*: SHA-256 content hash check on `(title + company + clean body)` to detect cross-posted listings.
5. **Discovery Dashboard**:
   * Frontend view to trigger discovery batches, view real-time ingestion counters, and inspect raw discovered postings.
