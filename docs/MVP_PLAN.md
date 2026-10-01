# JobHunter AI — MVP Implementation Plan & Milestones

## 1. MVP Scope & Boundaries

The objective of the Minimum Viable Product (MVP) is to deliver a rock-solid, production-grade implementation of the core value proposition: **Intelligent Job Search + Application Optimization**.

### 1.1 In-Scope MVP Capabilities
1. **Candidate Profile Management**: Create and edit structured candidate profile (Java, Spring Boot, React, Node.js, PostgreSQL, Docker, target CTC, locations).
2. **Master Resume Ingestion**: Upload and store immutable master resume (PDF/DOCX) with structured skills and experience parsing.
3. **Web Discovery via Firecrawl**: Perform targeted job searches via Firecrawl across key career sites and ATS boards.
4. **Clean Job Extraction**: Scrape clean markdown and metadata using Firecrawl.
5. **Two-Tier Deduplication**: Canonicalize URLs and compute SHA-256 hashes to enforce One Job = One Opportunity.
6. **Job Requirement Analysis**: Extract must-haves, nice-to-haves, and latent signals into a typed JSON schema.
7. **Semantic Matching**: Compare job requirements against candidate commercial experience and projects (Strong, Partial, Gaps, Transferable, Risks).
8. **Curated Job Prioritization**: Rank jobs by priority and fit (APPLY, APPLY_AFTER_TAILORING, LOW_PRIORITY, DO_NOT_APPLY).
9. **The Edge: Application Advantage Report**: Generate the strategic positioning report for matched jobs (priorities, relevance, evidence, framing).
10. **Tailored Resume Generator**: Produce grounded tailored resumes with an interactive Before/After diff viewer and ATS-compliant LaTeX/PDF export.
11. **Grounded Bullet Sharpening**: Align bullets with JD keywords strictly bounded by candidate verified ground truth.
12. **Assisted Application Workflow**: Launch the official application portal (`[Open Application Portal]`).
13. **Single Applied State Persistence**: Simple human-controlled applied toggle (`[Mark as Applied]`, `[Mark as Not Applied]`). Persists `applied: boolean` and `appliedAt: timestamp`.

### 1.2 Explicitly Out of Scope
* **Interview preparation**: Coaching, revision checklists, DSA/system design prep, or interview question generation.
* **Application lifecycle tracking**: Pipelines, stages (`INTERVIEW`, `OFFER`, `REJECTED`), status kanbans, application CRM.
* **Application outcome analytics**: Funnel conversion rates, interview-rate charts, response analytics.
* **Outcome-learning loops**: The system does NOT learn from rejections, interviews, or offers. Intelligence is derived directly from Candidate Profile + Master Resume + Projects + Job Description + Web Discovery + Semantic Matching.
* **Automated background submissions**: 100% human-approved; all applications are submitted manually by the candidate.

---

## 2. Phased Implementation Roadmap

```mermaid
gantt
    title JobHunter AI — Delivery Schedule
    dateFormat  YYYY-MM-DD
    section Delivered & Verified
    Milestone 1: Core Foundation & Profile API         :done, m1, 2026-09-28, 2d
    Milestone 2: Firecrawl Discovery & Deduplication  :done, m2, 2026-09-29, 2d
    Milestone 3: Semantic Matching & The Edge         :done, m3, 2026-09-29, 2d
    Milestone 4: Resume Tailoring & LaTeX/PDF Engine  :done, m4, 2026-09-30, 2d
    Scope Correction: Single Applied State & Application Assistance :done, m5_scope, 2026-09-30, 1d
    Milestone 5: Intelligent Job Ranking & Discovery Quality :done, m5, 2026-09-30, 1d
```

---

## 3. Milestone Summary & Status

### Milestone 1: Core Foundation & Candidate Profile
* **Goal**: Establish the full-stack architecture, PostgreSQL 16 schema, JWT authentication, and Candidate Knowledge Model.
* **Status**: **Completed & Verified**.

### Milestone 2: Firecrawl Web Intelligence & Job Discovery Engine
* **Goal**: Deploy web intelligence via Firecrawl, URL normalizer, two-tier deduplication, and Discovery Dashboard.
* **Status**: **Completed & Verified** (Live API key integrated and tested).

### Milestone 3: Semantic Job Understanding + Candidate Matching + The Edge
* **Goal**: Multi-dimensional requirement extraction, 768-D vector embeddings, Grounding Verification Gate, and Application Advantage Report ("The Edge").
* **Scope Boundary**: Strictly focused on job understanding, matching, and application positioning. Interview preparation is explicitly removed.
* **Status**: **Completed & Verified** (50 backend unit tests passing, clean frontend build).

### Milestone 4: Intelligent Resume Tailoring + Application Optimization
* **Goal**: Context-aware resume tailoring, grounded bullet point sharpening, Before/After diff viewer, and ATS-compliant LaTeX/PDF compilation via Apache PDFBox engine.
* **Scope Boundary**: Zero-tolerance hallucination policy strictly enforced; master resume immutable.
* **Status**: **Completed & Verified** (58 backend tests passing, clean frontend build).

### Final Scope Correction: Single Applied State & Application Launch
* **Goal**: Streamline application workflow to the single essential question: *"Have I already applied to this job?"*
* **Deliverables**:
  * Simplified `applications` table to `candidate_profile_id`, `job_id`, `applied: boolean`, `applied_at: timestamp`.
  * Removed all obsolete tracking tables (`interviews`, `interview_questions`, `interview_feedback`, `application_status_history`, `application_answers`, `application_documents`).
  * Enriched job APIs (`GET /api/jobs`, `GET /api/jobs/{id}`) with candidate's `applied` and `appliedAt` status.
  * Added `POST /api/jobs/{id}/applied` toggle endpoint.
  * Updated JobCard and JobDetailModal with `[Open Application Portal]`, `[Mark as Applied]`, and `[Mark as Not Applied]`.
  * Updated Discovery Dashboard with Job Discovery metrics and "Already Applied" filter.
* **Status**: **Completed & Verified** (61 backend tests passing, clean frontend build).

### Milestone 5: Intelligent Job Ranking & Discovery Quality
* **Goal**: Determine which jobs discovered from the internet are genuinely worth the candidate's attention and explain why ("Why should this candidate spend time on this job?").
* **Deliverables**:
  * **Strict Distinction between Match & Priority**: Match = semantic correspondence; Priority = curated candidate attention based on match, hard constraints, freshness, and quality.
  * **Deterministic Priority Categorization**: `HIGH_PRIORITY`, `MEDIUM_PRIORITY`, `LOW_PRIORITY`, `NOT_RECOMMENDED`.
  * **Freshness Rigor**: `NEW` (<=7d), `RECENT` (8-30d), `OLDER` (31-60d), `STALE` (>60d, automatically downranked), `UNKNOWN` (source did not disclose date; never fabricated).
  * **Hard Constraints Override Soft Signals**: Non-negotiable blockers (Seniority gap >= 2.5 yrs, executive title barrier, missing mandatory domain tech, strict on-site location mismatch) cap priority at `NOT_RECOMMENDED` or `LOW_PRIORITY`.
  * **Evidence-Backed Explanations**: "Why This Job?" highlights direct verified commercial experience and preferences; "Potential Concerns" transparently flags gaps, staleness, and constraint violations.
  * **Key Technology Coverage**: Top 5 critical stack items on JobCard with visual indicators (`✓` Strong commercial, `△` Partial project, `✕` Gap).
  * **Zero Hiring Probability**: Never predicts employer behavior or claims offer likelihood.
  * **Independence of Applied State**: Prioritization is strictly invariant to `applied` boolean state.
  * **3-Tier Query Generation**: EXACT, ADJACENT, and SKILL_LED search strategies across Greenhouse, Lever, Ashby, and Workday.
  * **Tier 3 Cross-Source Deduplication**: Token-level Jaccard similarity (>75%) for company and title, preserving distinct office locations.
  * **Frontend UI Enhancements**: Priority Category badges, Freshness tags, Key Tech chips, Why This Job highlight, Concerns callout, Priority & Freshness filter toolbars, and Sort By selector (`RECOMMENDED`, `NEWEST`, `BEST_MATCH`, `ALREADY_APPLIED`, `MOST_RELEVANT`).
* **Status**: **Completed & Verified** (71 backend tests passing, clean frontend build).

---

## 4. Verification & Testing Strategy

1. **Unit Testing**:
   * JUnit 5 + Mockito for Java business logic (Deduplication, Grounding Gate, Matching formulas, Tailoring, Applied state).
2. **Integration Testing**:
   * WireMock and live API tests for Firecrawl and LLM providers.
3. **E2E Verification**:
   * Complete pipeline verification with verified candidate profiles and live scraped job postings.
