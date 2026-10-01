# JobHunter AI — Milestone 3 Implementation Report
## Semantic Job Understanding + Candidate Matching + The Edge System

**Implementation Date:** September 29, 2026  
**Status:** Completed, Verified & Scope-Aligned  
**Previous Milestones:** Milestone 1 (Foundation + Profile + Auth), Milestone 2 (Firecrawl Web Intelligence + Job Discovery)  
**Current Milestone:** Milestone 3 (Semantic Job Understanding + Candidate Matching + The Edge)  
**Product Category:** Intelligent Job Search + Application Optimization Platform (Interview Preparation Out of Scope)

---

### Executive Summary

Milestone 3 marks the qualitative transition of **JobHunter AI** from a discovery tool into an intelligent, high-agency career copilot. Where Milestone 2 answered *"What jobs exist across the web?"*, Milestone 3 answers:

> *"Which jobs on the internet are worth my attention, why am I actually relevant to them, and how should I position my real experience when applying?"*

Every match assessment, priority score, and positioning recommendation is governed by a **Zero-Tolerance Hallucination Philosophy**. The backend enforces a deterministic **Grounding Verification Gate** in Java that strictly audits claims against the candidate's verified profile, commercial experiences, and portfolio projects before any recommendation is rendered.

Per product scope decisions, JobHunter AI is strictly an **Intelligent Job Search + Application Optimization Platform** and is **NOT** an interview preparation tool. All interview question generation, coaching, revision checklists, and study plans have been eliminated.

---

### Key Architectural Deliverables

```
+--------------------------------------------------------------------------------------------------+
|                                    JOBHUNTER AI - MILESTONE 3                                     |
+--------------------------------------------------------------------------------------------------+
|                                                                                                  |
|   1. Grounding Verification Gate (Java)                                                          |
|      * Validates every technical requirement claim against candidate profile, experiences &     |
|        projects.                                                                                 |
|      * Rejects ungrounded technologies (converting them to verifiable GAPs).                     |
|      * Demotes project-only skills if claimed as commercial experience.                          |
|                                                                                                  |
|   2. Universal Vector Service & Embeddings (768-D)                                               |
|      * Pluggable EmbeddingProvider SPI supporting Gemini text-embedding-004.                     |
|      * Deterministic Local Provider via term-frequency feature hashing + L2 normalization        |
|        (100% deterministic, zero random or faked numbers).                                       |
|      * PostgreSQL Environment Check: Auto-detects pgvector; falls back to universal JSONB        |
|        storage with exact in-memory dot-product cosine similarity when pgvector is absent.        |
|                                                                                                  |
|   3. Multi-Dimensional Semantic Matching Engine                                                  |
|      * Deep requirement extraction: Must-Have, Nice-To-Have, Responsibilities, Seniority,       |
|        Domain, Work Mode, Location, and Implied Requirements.                                    |
|      * Token-boundary keyword regex (\b) eliminating false positives (e.g. React vs reactive).   |
|      * Multi-factor Composite Priority Scoring (0-100%):                                         |
|          Skill Coverage (40%) + Experience/YOE (25%) + Cosine Sim (15%) +                         |
|          Location/Mode (10%) + Evidence Depth (10%).                                             |
|      * Transparent Tiers: APPLY, APPLY_AFTER_TAILORING, LOW_PRIORITY, DO_NOT_APPLY.              |
|                                                                                                  |
|   4. The Edge System (10-Dimension Application Advantage Report)                                 |
|      * 10 tactical dimensions: Employer Priorities, Candidate Relevance, Strongest Evidence,     |
|        What to Emphasize, What to De-emphasize, Honest Gaps, Transferable Skills,                 |
|        Resume Positioning, Application Fit & Positioning, Application Strategy.                  |
|                                                                                                  |
|   5. Resume Tailoring Engine Foundation                                                          |
|      * Grounded bullet sharpening proposals citing verified career evidence.                     |
|      * Section reordering recommendations and ATS keyword rationale.                             |
|      * Exposed via GET /api/jobs/{id}/tailoring.                                                 |
|                                                                                                  |
|   6. Enhanced React UI & Career Copilot                                                          |
|      * 4-tab JobDetailModal (Overview, Match Analysis, The Edge, Resume Tailoring).              |
|      * Color-coded recommendation pills, priority score gauge, and cosine similarity pill.       |
|      * Grounded requirement-by-requirement audit table with status filter pills.                 |
|      * JobCard priority score progress bar & quick recommendation badges.                        |
|      * Dashboard match filter pills (All, Strong Match, Tailor, Low/Skip).                       |
|      * Human-In-The-Loop: External application portal opened in new tab.                          |
+--------------------------------------------------------------------------------------------------+
```

---

### 1. Database Architecture & Flyway Migration V2

Flyway migration [`V2__milestone3_semantic_matching.sql`](file:///c:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/resources/db/migration/V2__milestone3_semantic_matching.sql) was developed and applied to PostgreSQL 16:

#### Tables Created / Altered:
1. **`candidate_experiences`**: Structured work history tracking company, role, dates, description, achievements (JSONB array), and technologies used (JSONB array).
2. **`candidate_projects`**: Portfolio projects tracking name, problem solved, architecture overview, key contributions (JSONB), measurable metrics (JSONB), and repository URL.
3. **`candidate_skills`**: Altered with `experience_type` (`COMMERCIAL`, `PROJECT_ONLY`, `LEARNING`), `confidence` (numeric 0.00-1.00), and `evidence_source`.
4. **`job_requirements`**: Altered with `is_implied` (boolean) and `raw_text_snippet`.
5. **`job_embeddings`**: Universal vector storage table (`embedding_json` JSONB, model name, dimensions = 768).
6. **`candidate_embeddings`**: Universal vector storage table for candidate profile state (`embedding_json` JSONB, model name, dimensions = 768).

#### PostgreSQL pgvector Status Report:
- **Detection Method:** Verified via query `SELECT * FROM pg_available_extensions WHERE name = 'vector';`.
- **Finding:** The native `vector` extension is **NOT** installed in the local PostgreSQL 16 instance on Windows.
- **Architectural Solution:**
  - The system implements **Universal JSONB Vector Storage**. Float arrays (`float[]` of 768 dimensions) are serialized to JSONB.
  - Cosine similarity is computed via high-performance dot-product:
    $$\text{sim}(A, B) = \frac{\sum_{i=1}^{768} A_i B_i}{\sqrt{\sum_{i=1}^{768} A_i^2} \sqrt{\sum_{i=1}^{768} B_i^2}}$$
  - **Zero Faking Guarantee:** Embeddings are **NEVER** generated using random numbers. When Gemini API keys are unconfigured, `DeterministicLocalEmbeddingProvider` generates deterministic 768-D L2-normalized vectors via Murmur-inspired positive term frequency feature hashing.
  - **Forward Compatibility:** `V2__milestone3_semantic_matching.sql` contains conditional blocks (`DO $$ BEGIN ... EXCEPTION WHEN ... END $$;`) that will automatically add native `vector(768)` columns with `hnsw` indexes if pgvector is installed in a target cloud deployment.

---

### 2. Grounding Verification Gate (Zero-Tolerance Hallucination)

Implemented in [`GroundingVerificationGate.java`](file:///c:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/service/matching/GroundingVerificationGate.java).

#### Enforcement Rules:
1. **Commercial vs Project-Only Demotion:** If a claim asserts commercial production mastery of a technology that is only present in personal portfolio projects, the gate demotes the claim from `COMMERCIAL` to `PROJECT_ONLY` and lowers the match type from `STRONG` to `PARTIAL`.
2. **Ungrounded Claim Rejection:** If a claim asserts familiarity with a skill that has zero trace in the candidate's verified skills, experience bullet points, or projects, the claim is rejected outright and converted into an honest `GAP`.
3. **Traceable Evidence Anchors:** Every strong or partial match requirement is linked to verified evidence (e.g. `2.5 years commercial experience in Java (ADVANCED). Core Java and backend REST microservices`).

---

### 3. The Edge System: 10-Dimension Application Advantage Report

Implemented in [`ApplicationAdvantageReportDto.java`](file:///c:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/dto/matching/ApplicationAdvantageReportDto.java) and synthesized in [`SemanticMatchingService.java`](file:///c:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/service/matching/SemanticMatchingService.java).

Strictly focused on application positioning and tactical advantage:
1. **Dimension 1 — Employer Priorities**: Highest-signal architectural and engineering focus extracted from JD.
2. **Dimension 2 — Candidate Relevance**: Verified commercial narrative linking background to role.
3. **Dimension 3 — Strongest Evidence**: Verified projects, production responsibilities, and metrics.
4. **Dimension 4 — What to Emphasize**: High-signal competencies that deserve top visual hierarchy.
5. **Dimension 5 — What to De-emphasize**: Irrelevant secondary technologies to remove or compress.
6. **Dimension 6 — Honest Gaps**: Candid, transparent acknowledgment of missing qualifications.
7. **Dimension 7 — Transferable Skills**: Legitimate adjacent capabilities bridging secondary requirements.
8. **Dimension 8 — Resume Positioning**: Concrete recommendations for section order and skill bar layout.
9. **Dimension 9 — Application Fit & Positioning**: Strategic angle for how the candidate should be perceived.
10. **Dimension 10 — Application Strategy**: Actionable steps for submitting through the employer's official portal.

---

### 4. REST API Endpoints

All endpoints are secured via JWT authentication (`Authorization: Bearer <token>`):

1. **`POST /api/jobs/{id}/analyze`**
   - Forces live re-evaluation of semantic similarity, requirement extraction, grounding audit, and advantage synthesis.
   - Saves result to `job_matches` and updates job pipeline status to `ANALYZED`.
2. **`GET /api/jobs/{id}/match`**
   - Returns cached or fresh `MatchAnalysisResponse` including the complete requirement-by-requirement audit table.
3. **`GET /api/jobs/{id}/requirements`**
   - Returns extracted `JobRequirementResponse` list with inferred importance and raw snippets.
4. **`GET /api/jobs/{id}/advantage`**
   - Returns 10-dimension `ApplicationAdvantageReportDto` (The Edge System).
5. **`GET /api/jobs/{id}/tailoring`**
   - Returns `TailoringRecommendationDto` containing section reordering advice, skills to feature/de-emphasize, and grounded bullet sharpening proposals.

---

### 5. React Matching UI & Career Copilot

#### `JobDetailModal.tsx`:
- **Header:** Live recommendation badge (`APPLY`, `APPLY_AFTER_TAILORING`, etc.), queue tier, priority score, and one-click `[Re-analyze]` action.
- **Tab 1: Overview:** Compensation, experience, posting date, employment type, detected technologies, cleaned markdown description, and human-in-the-loop notice.
- **Tab 2: Match Analysis:**
  - Multi-dimensional score banner (Priority Score + Vector Cosine Sim).
  - Overall assessment narrative.
  - 4-card metric overview: Strong Matches, Transferable Skills, Gaps, Risk Factors.
  - Filterable Grounded Requirement Audit Table (`All`, `Strong`, `Partial`, `Transferable`, `Gap`).
- **Tab 3: The Edge (Advantage Report):** Beautiful rendering of all 10 application positioning dimensions.
- **Tab 4: Resume Tailoring:** Bullet point sharpening proposals with ATS keyword rationale, section reordering advice, and Milestone 4 LaTeX deferral notice.
- **Footer:** Preserves original ATS link with `[Open Application Portal]` (`target="_blank" rel="noopener noreferrer"`).

#### `JobCard.tsx`:
- Displays top score progress bar colored by recommendation tier.
- Displays recommendation pill with score (e.g., `✓ 85% Apply`, `⚡ 53% Tailor`, `⚠ 42% Low`, `✕ 20% Skip`).

#### `DiscoveryDashboard.tsx`:
- Added Match Filter pills (`All`, `Strong Match (APPLY)`, `Tailor (Review)`, `Low / Skip`).
- Asynchronously loads and caches match evaluations for all discovered jobs.

---

### 6. Verification & Automated Test Results

#### Backend Test Suite:
```
[INFO] Results:
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 31.257 s
```
- `EmbeddingProviderTest`: 4 tests passing
- `VectorServiceTest`: 4 tests passing
- `JobRequirementExtractorTest`: 2 tests passing
- `GroundingVerificationGateTest`: 3 tests passing
- `SemanticMatchingServiceTest`: 6 tests passing (Strong, Partial, Poor, Missing Data, 10 Dimensions Advantage, Tailoring Recommendations)
- Existing Milestone 1 & 2 tests: 31 tests passing

#### Frontend Production Build:
```
> tsc && vite build
vite v5.4.21 building for production...
✓ 1631 modules transformed.
dist/index.html                   0.89 kB │ gzip:  0.51 kB
dist/assets/index-CwFPfFvP.css   30.25 kB │ gzip:  5.95 kB
dist/assets/index-0xl5aFKC.js   294.42 kB │ gzip: 85.23 kB
✓ built in 3.32s
```

---

### Summary Checklist

- [x] Zero-tolerance hallucination policy strictly enforced via Java Grounding Gate.
- [x] Pluggable 768-D `EmbeddingProvider` with deterministic local fallback.
- [x] PostgreSQL environment checked; universal JSONB fallback active.
- [x] Transparent requirement-by-requirement audit (`STRONG`, `PARTIAL`, `TRANSFERABLE`, `GAP`).
- [x] Multi-factor Composite Priority Scoring (0-100%).
- [x] 10-dimension Application Advantage Report (The Edge) strictly focused on application positioning.
- [x] Interview preparation completely removed from backend, frontend, and documentation.
- [x] Resume Tailoring recommendations and bullet sharpening proposals prepared and exposed via `/api/jobs/{id}/tailoring`.
- [x] 5 secured REST API endpoints tested and responsive.
- [x] 4-tab React Matching UI with recommendation badges and audit table.
- [x] 50 backend tests passing with 0 failures, 0 errors.
- [x] Frontend production build successful (`tsc && vite build`).
- [x] Execution stopped; awaiting user approval before Milestone 4.
