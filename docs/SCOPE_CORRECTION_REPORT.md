# JobHunter AI — Product Scope Correction Report
## Realigning to Intelligent Job Search + Application Optimization Platform

**Correction Date:** September 29, 2026  
**Status:** Complete, Verified & Cleaned  
**Product Category:** Intelligent Job Search + Application Optimization Platform  
**Explicit Out-of-Scope Domain:** Interview Preparation  

---

### 1. Executive Summary of Scope Correction

JobHunter AI has executed a definitive product-scope correction to remain razor-focused on its core mission:

> *"Which jobs on the internet are worth my attention, why am I actually relevant to them, and how should I position my real experience when applying?"*

JobHunter AI is **NOT an interview preparation platform**. It does not generate interview questions, technical revision checklists, behavioral prep, DSA/system design guides, or study roadmaps. 

All interview-preparation features, endpoints, DTOs, and UI components introduced during initial development have been systematically removed from the codebase. The engineering focus is permanently aligned to high-conviction job discovery, semantic job understanding, truthful evidence matching, application positioning ("The Edge"), and tailored application collateral.

---

### 2. Removed Functionality

The following components and references have been completely excised from the codebase:

| Category | Component / Artifact | Action Taken |
| :--- | :--- | :--- |
| **Backend REST API** | `GET /api/jobs/{id}/interview-prep` | **Removed** from `JobController.java` |
| **Backend DTO** | `InterviewPreparationPackDto.java` | **Deleted** entirely |
| **Backend Service** | `SemanticMatchingService.getInterviewPrep` | **Removed** from `SemanticMatchingService.java` |
| **Backend Service** | `SemanticMatchingService.deriveInterviewPreparationAreas` | **Removed** from `SemanticMatchingService.java` |
| **Backend DTO** | `MatchAnalysisResponse.interviewPreparationAreas` | **Removed** property, getter, and setter |
| **Backend DTO** | `ApplicationAdvantageReportDto.interviewPreparation` | **Removed**; replaced with `applicationFitAndPositioning` |
| **Frontend Types** | `InterviewQuestionItem`, `InterviewPreparationPack` | **Removed** from `frontend/src/types/index.ts` |
| **Frontend API** | `apiClient.matching.getInterviewPrep` | **Removed** from `frontend/src/api/client.ts` |
| **Frontend UI** | `JobDetailModal` Interview Prep Tab & State | **Removed** tab button, loading state, and render block |
| **Frontend UI** | `Navbar` "Interview Prep (M6)" link | **Removed** from `frontend/src/components/Navbar.tsx` |
| **Agent Design** | `InterviewPrepAgent` | **Removed** from `docs/AGENT_ARCHITECTURE.md` |
| **Roadmap** | Milestone 6 "Interview Prep" description | **Refactored** in `docs/MVP_PLAN.md` to Outcome Tracking |

---

### 3. Preserved & Strengthened Functionality

All valuable discovery, semantic matching, and evidence-grounding architectures remain 100% operational:

1. **Firecrawl Web Intelligence & Live Discovery Engine:**
   - Firecrawl API key configured via environment variable `FIRECRAWL_API_KEY`.
   - Live discovery searches and scrapes ATS career boards (Greenhouse, Lever, Ashby, Workday, etc.).
   - Two-tier deduplication (SHA-256 canonical URL hash and content hash) enforcing **One Job = One Opportunity**.
2. **Semantic Job Understanding & Requirement Extraction:**
   - Extracts must-haves, nice-to-haves, responsibilities, seniority, domain, work mode, and implied requirements.
   - Token-boundary regex scoping (`\b`) preventing false positives (e.g., distinguishing "React" from "reactive").
3. **Zero-Tolerance Hallucination / Grounding Verification Gate:**
   - Programmatic Java validator ([`GroundingVerificationGate.java`](file:///c:/Users/nairy/.gemini/antigravity/scratch/jobhunter-ai/backend/src/main/java/com/jobhunter/service/matching/GroundingVerificationGate.java)) cross-referencing all claims against candidate skills, experiences, and projects.
   - Demotes project-only skills if claimed as commercial experience; flags and rejects ungrounded skills as honest gaps.
4. **Universal Vector Embeddings (768-D):**
   - Pluggable `EmbeddingProvider` SPI with deterministic local term-frequency feature hashing fallback.
   - Universal JSONB vector persistence with high-performance dot-product cosine similarity.
5. **Multidimensional Semantic Matching Engine:**
   - 4-tier recommendation categories: `APPLY`, `APPLY_AFTER_TAILORING`, `LOW_PRIORITY`, `DO_NOT_APPLY`.
   - Seniority delta penalties and work mode friction detection.
6. **The Edge: 10-Dimension Application Advantage Report:**
   - Re-centered strictly around application positioning:
     1. *Employer Priorities*
     2. *Candidate Relevance*
     3. *Strongest Evidence*
     4. *What to Emphasize*
     5. *What to De-emphasize*
     6. *Honest Gaps*
     7. *Transferable Skills*
     8. *Resume Positioning*
     9. *Application Fit & Positioning*
     10. *Application Strategy*
7. **Resume Tailoring Foundation:**
   - Section reordering and bullet sharpening proposals exposed via `GET /api/jobs/{id}/tailoring`.
8. **Human-in-the-Loop Application Flow:**
   - `[Open Application Portal]` opens external ATS URLs in a new browser tab (`target="_blank" rel="noopener noreferrer"`).
   - Strict ban on silent, unauthorized background auto-submitting.

---

### 4. Updated Product Boundary & Core Pipeline

```
DISCOVER  ──►  FILTER  ──►  UNDERSTAND  ──►  MATCH  ──►  EXPLAIN
   │
   ▼
PRIORITIZE ──►  TAILOR  ──►  APPLY  ──►  TRACK  ──►  LEARN
```

#### Application Tracking vs. Interview Coaching Distinction:
* **In Scope**: Tracking application status (`SHORTLISTED` → `TAILORED` → `APPLIED` → `INTERVIEW` → `OFFER` / `REJECTED`). Outcome data feeds empirical conversion metrics and refines future search queries.
* **Out of Scope**: Coaching, study plans, DSA problem sets, system design revision, or behavioral interview mock questions.

---

### 5. Verification & Test Results

#### A. Backend Unit & Integration Tests
* **Command:** `.\mvnw.cmd test`
* **Result:** **50 / 50 Tests Passed** (0 Failures, 0 Errors, 0 Skipped).
* **Execution Time:** ~31 seconds.

```
[INFO] Results:
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Key suites verified:
* `SemanticMatchingServiceTest`: 6 tests passing (including 10-dimension advantage report and tailoring recommendations).
* `GroundingVerificationGateTest`: 3 tests passing (commercial demotion, ungrounded claim rejection).
* `EmbeddingProviderTest` & `VectorServiceTest`: 8 tests passing.
* `DiscoveryServiceTest`, `DeduplicationServiceTest`, `JobQualityFilterTest`: 21 tests passing.
* `FirecrawlClientTest`: 5 tests passing.

#### B. Frontend Production Build
* **Command:** `npm run build` (`tsc && vite build`)
* **Result:** **Built in 3.32s with 0 errors**.
* **Modules Transformed:** 1,631 modules.

```
vite v5.4.21 building for production...
✓ 1631 modules transformed.
dist/index.html                   0.89 kB │ gzip:  0.51 kB
dist/assets/index-CwFPfFvP.css   30.25 kB │ gzip:  5.95 kB
dist/assets/index-0xl5aFKC.js   294.42 kB │ gzip: 85.23 kB
✓ built in 3.32s
```

#### C. Live Services Status
* **Spring Boot Backend:** Running on `http://localhost:8085` (PID active, Flyway V2 applied, Firecrawl live key wired).
* **Vite Dev Server:** Running on `http://localhost:5173` (HMR active).

---

### 6. Remaining Milestone 3 Functionality Overview

The revised and verified Milestone 3 deliverables now include:
1. **Semantic Matching Engine:** Evaluates candidate profiles against discovered vacancies with 768-D embeddings and multi-factor scoring.
2. **Grounding Verification Gate:** Zero-hallucination auditor operating in Java.
3. **Transparent Audit Table:** Interactive requirement breakdown (`STRONG`, `PARTIAL`, `TRANSFERABLE`, `GAP`).
4. **The Edge Advantage Report:** 10 application positioning dimensions.
5. **Resume Tailoring Endpoint:** `/api/jobs/{id}/tailoring` providing section reordering advice and bullet sharpening proposals.
6. **Copilot UI:** 4-tab `JobDetailModal` (Overview, Match Analysis, The Edge, Resume Tailoring), recommendation pills, and match filter pills.

---

### 7. Recommended Next Milestone: Milestone 4

With interview preparation eliminated and the product boundary restored, the platform is ready for:

**MILESTONE 4 — TAILORED APPLICATION COLLATERAL ENGINE**
1. **Targeted Resume Tailoring**: Context-aware section reordering and bullet point sharpening strictly rooted in verified candidate ground truth.
2. **Cover Letter Generator**: Role- and company-specific outreach letters synthesized from "The Edge" report.
3. **Multi-Format Export**: Production-ready LaTeX rendering and direct PDF generation.
4. **Application QA Generator**: Concise, grounded answers for standard application portal questions.

---

### 8. Milestone Stop

Per user instructions, development has stopped after completing this cleanup. Awaiting user review and authorization before initiating Milestone 4.
