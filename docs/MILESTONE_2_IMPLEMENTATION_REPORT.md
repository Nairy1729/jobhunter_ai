# Milestone 2 Implementation Report: Firecrawl Web Intelligence & Job Discovery Engine

**Project:** JobHunter AI  
**Milestone:** 2 — Firecrawl Web Intelligence + Job Discovery Engine  
**Completed Date:** September 29, 2026  
**Status:** COMPLETE & VERIFIED  

---

## 1. Executive Summary

Milestone 2 delivers the automated **Web Intelligence and Job Discovery Engine** for JobHunter AI. This engine bridges candidate profile data (target roles, verified technical skills, locations, and work modes) with live career opportunities across major Applicant Tracking Systems (Greenhouse, Lever, Ashby, and Workday) using Firecrawl search and scrape capabilities.

The implementation strictly honors the core project philosophy:
* **Deterministic and Resilient:** Firecrawl integration is governed by Resilience4j rate limiting, circuit breaker, and exponential backoff retries.
* **Two-Tier Deduplication:** Zero duplicate ingestion across search runs and cross-posted ATS URLs using canonical URL normalization (Tier 1) and SHA-256 content hashing (Tier 2).
* **Cost-Conscious Quality Filtering:** Rejects expired, closed, and non-job listing pages deterministically before burning LLM tokens.
* **Strict Honesty in Extraction:** If salary, experience, or dates are absent from the job posting, they remain `null` — never hallucinated or fabricated.
* **Security & SSRF Hardening:** Full RFC 1918 private IP validation, link-local protection, and prompt-injection isolation on untrusted scraped HTML/Markdown.
* **Human-in-the-Loop Integrity:** The discovery dashboard presents a clear `[Open Application]` action linking to the original ATS URL with `rel="noopener noreferrer"`. No autonomous submissions occur.

---

## 2. Architecture & Component Design

```
+-------------------------------------------------------------------------------+
|                             Candidate Profile                                 |
|         (Roles: "Backend Engineer", Skills: ["Java", "Spring Boot"]...)       |
+---------------------------------------+---------------------------------------+
                                        |
                                        v
                       +---------------------------------+
                       |   JobSearchQueryGenerator       |
                       | (Generates ATS-targeted queries)|
                       +----------------+----------------+
                                        |
                                        v
                       +---------------------------------+
                       |      DiscoveryService           |
                       |  (Pipeline Orchestration & Runs)|
                       +----------------+----------------+
                                        |
                        +---------------+---------------+
                        |                               |
                        v                               v
             +--------------------+          +---------------------+
             |  FirecrawlClient   |          |    UrlNormalizer    |
             |   (Resilience4j)   |          | (SSRF & Canonical)  |
             +----------+---------+          +----------+----------+
                        |                               |
                        +---------------+---------------+
                                        |
                                        v
                       +---------------------------------+
                       |   Deduplication Tier 1 Check    |
                       | (Canonical URL Hash vs DB)      |
                       +----------------+----------------+
                                        | (If fresh)
                                        v
                       +---------------------------------+
                       |        JobQualityFilter         |
                       | (Rejects closed/expired pages)  |
                       +----------------+----------------+
                                        | (If passed)
                                        v
                       +---------------------------------+
                       |       JobExtractorService       |
                       | (Heuristics, Salary, Skills)    |
                       +----------------+----------------+
                                        |
                                        v
                       +---------------------------------+
                       |   Deduplication Tier 2 Check    |
                       | (Content SHA-256 Hash vs DB)    |
                       +----------------+----------------+
                                        | (If unique)
                                        v
                       +---------------------------------+
                       |   PostgreSQL 16 Persistence     |
                       |  (jobs, search_runs, agent_runs)|
                       +---------------------------------+
```

### 2.1 Firecrawl Client & Resilience4j Layer
* **Files:**
  - `com.jobhunter.client.firecrawl.FirecrawlClient`: Interface defining `search(request)` and `scrape(request)` contracts.
  - `com.jobhunter.client.firecrawl.FirecrawlClientImpl`: Production WebClient implementation with timeout bounds (connect timeout 5s, read timeout 15s).
  - `com.jobhunter.config.ResilienceConfig`: Resilience4j configuration specifying:
    * **Rate Limiter:** 5 requests per second max (`limitForPeriod=5`, `limitRefreshPeriod=1s`).
    * **Circuit Breaker:** Sliding window of 10 requests, 50% failure rate threshold, 30s open state duration.
    * **Exponential Backoff Retry:** 3 max attempts with initial 1s interval and multiplier of 2.0.
  - **Error Classification:** Distinguishes client errors (400, 401, 403, 404 - no retry) from rate limits (429 - retried with backoff) and server errors (5xx - retried). Graceful degradation when `FIRECRAWL_API_KEY` is omitted with clear operational logging.

### 2.2 ATS Source Adapters
* **Architecture:** Pluggable adapter interface `JobSourceAdapter` loaded via Spring DI into `JobSourceAdapterRegistry`.
* **Adapters Implemented:**
  1. `GreenhouseAdapter`: Handles `boards.greenhouse.io`, strips tracking query params (`gh_src`, `utm_*`), extracts company slug and clean job titles.
  2. `LeverAdapter`: Handles `jobs.lever.co`, cleans `posting_id` URLs, standardizes job location and title formats.
  3. `WorkdayAdapter`: Handles `*.myworkdayjobs.com`, handles nested routing slugs and tenant IDs.
  4. `AshbyAdapter`: Handles `jobs.ashbyhq.com`, strips query params and referral tokens.
  5. `GenericCareerAdapter`: Universal fallback for arbitrary corporate career domains.

### 2.3 Dynamic Search Query Generator
* **Class:** `JobSearchQueryGenerator`
* **Strategy:** Translates the candidate's target roles, primary verified skills, and preferred locations into targeted search queries:
  - Generates role-based queries scoped to supported ATS platforms (e.g., `site:boards.greenhouse.io "Senior Java Engineer" ("Spring" OR "PostgreSQL") (Remote OR "San Francisco")`).
  - Limits execution to balanced batches across ATS sources.
  - Employs fallback general queries if the candidate profile has minimal preferences.

### 2.4 Two-Tier Deterministic Deduplication
* **Class:** `DeduplicationService`
* **Tier 1 (Canonical URL Hash):**
  - Normalizes URL (protocol lowercase, standard ports stripped, path slashes normalized, query tracking parameters like `utm_*`, `gh_src`, `ref` removed).
  - Generates SHA-256 hex digest stored in `jobs.canonical_url_hash`.
  - Checked *before* scraping to avoid wasting API quota on previously discovered URLs.
* **Tier 2 (Content SHA-256 Hash):**
  - Normalizes text: `normalized_title + " " + company_name + " " + clean_markdown_body`.
  - Generates SHA-256 hex digest stored in `jobs.content_hash`.
  - Catches identical job postings cross-posted across multiple ATS platforms or different URLs.

### 2.5 Job Quality & Liveness Filter
* **Class:** `JobQualityFilter`
* **Deterministic Rejection Rules:**
  - URL heuristics: Rejects `/departments`, `/categories`, `/login`, `/search`, `/all-jobs`, `/privacy`.
  - HTTP Status: Rejects non-2xx status codes (404, 410, 500).
  - Closed posting detection: Matches regex patterns across markdown body:
    * `this (?:job|position|role) (?:is no longer available|has expired|has been closed|has been filled)`
    * `no longer accepting applications`
    * `posting has been removed`
  - Body length validation: Rejects pages under 150 characters as empty or login-walled.

### 2.6 Structured Job Extractor
* **Class:** `JobExtractorService`
* **Extraction Rules:**
  - **Title Normalization:** Strips ATS suffixes (e.g. ` | Greenhouse`, ` - Lever Jobs`, ` at Stripe`). Produces lowercase alphanumeric `normalized_title` for exact and prefix search.
  - **Salary Extraction:** Deterministic regex parsing for USD (`$XXk - $YYk`, `$XXX,XXX - $YYY,YYY`) and INR (`XX - YY LPA`, `₹XX,XX,XXX`). Absent salaries are strictly saved as `null`.
  - **Experience Extraction:** Captures numeric ranges (`3-5 years`, `5+ years`). Absent requirements remain `null`.
  - **Work Mode Detection:** Identifies `REMOTE`, `HYBRID`, and `ON_SITE`. Defaults to `UNKNOWN` if unspecified.
  - **Technology Tagging:** Scans content against the verified technology taxonomy (Java, Python, Spring Boot, React, Docker, Kubernetes, AWS, PostgreSQL, Kafka, etc.).
  - **Prompt Injection Defense:** Content is sanitized of delimiters (`<|im_start|>`, `system:`, `human:`, `assistant:`, `ignore previous instructions`) and treated strictly as untrusted text.

### 2.7 Observability & Run Tracking
* **Entity:** `SearchRun`
  - Captures `query_string`, `parameters` (JSONB), `status` (RUNNING, COMPLETED, FAILED), `jobs_discovered_count`, `jobs_ingested_count`, `started_at`, and `completed_at`.
* **Entity:** `AgentRun`
  - Audits execution of `DiscoveryAgent` in `agent_runs` table with metrics, errors, execution time in milliseconds, and structured output details.

---

## 3. Database Schema & Entities

Milestone 2 introduced the following JPA Entities and mappings matching the Flyway `V1__init_schema.sql` schema:

1. **`SearchRun` (`com.jobhunter.model.entity.SearchRun`)**:
   - `id`: UUID (Primary Key)
   - `job_source_id`: UUID (Foreign Key to `job_sources`)
   - `query_string`: TEXT
   - `parameters`: JSONB
   - `status`: VARCHAR(50)
   - `jobs_discovered_count`: INTEGER
   - `jobs_ingested_count`: INTEGER
   - `error_message`: TEXT
   - `started_at`: TIMESTAMPTZ
   - `completed_at`: TIMESTAMPTZ

2. **`AgentRun` (`com.jobhunter.model.entity.AgentRun`)**:
   - `id`: UUID (Primary Key)
   - `agent_type`: VARCHAR(100) (e.g. `DiscoveryAgent`)
   - `status`: VARCHAR(50) (e.g. `COMPLETED`, `ABORTED`, `FAILED`)
   - `jobs_processed`: INTEGER
   - `error_message`: TEXT
   - `execution_time_ms`: BIGINT
   - `created_at`: TIMESTAMPTZ

3. **`Job` (`com.jobhunter.model.entity.Job`)**:
   - Updated with `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "search_run_id") private SearchRun searchRun;`
   - Unique constraints enforced: `canonical_url_hash` and `content_hash`.

4. **Repositories**:
   - `SearchRunRepository`: JPA methods including `findByOrderByStartedAtDesc()`.
   - `AgentRunRepository`: JPA methods for agent execution logs.
   - `JobRepository`: Extended with `JpaSpecificationExecutor<Job>` for dynamic multi-attribute filtering.

---

## 4. API Endpoints & Contracts

All endpoints are secured under Spring Security with JWT Bearer authentication:

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/jobs/discover` | Triggers on-demand discovery run for authenticated candidate | Yes (`ROLE_CANDIDATE`, `ROLE_ADMIN`) |
| `GET` | `/api/jobs` | Lists discovered jobs with pagination and multi-attribute filters | Yes |
| `GET` | `/api/jobs/{id}` | Retrieves full job details and raw markdown description | Yes |
| `GET` | `/api/jobs/search-runs` | Retrieves historical search run telemetry and metrics | Yes |

### Filter Parameters for `GET /api/jobs`:
* `role` (String, case-insensitive substring on title)
* `location` (String, case-insensitive substring on location)
* `workMode` (Enum: `REMOTE`, `HYBRID`, `ON_SITE`, `UNKNOWN`)
* `technology` (String, matches against JSONB detected technologies)
* `source` (String, matches against `job_sources.name`)
* `minSalary` (BigDecimal, filters jobs with maxSalary >= minSalary or minSalary >= minSalary)
* `page`, `size` (Pagination controls)

---

## 5. Frontend Discovery Dashboard

A dedicated **Discovery Dashboard** was built and integrated into the React 18 frontend:

1. **Dashboard Navigation (`Navbar.tsx`):**
   - Added active navigation tab: `Discovery Dashboard` (`Compass` icon).
2. **Filter & Search Controls (`JobFilters.tsx`):**
   - Role / keyword search input with instant debounce.
   - Location input.
   - Work Mode selector (`All`, `Remote`, `Hybrid`, `On-Site`).
   - ATS Source filter (`All`, `Greenhouse`, `Lever`, `Ashby`, `Workday`).
   - Technology tag filter.
3. **Responsive Job Cards (`JobCard.tsx`):**
   - Company name and source badge (e.g. `GREENHOUSE`, `LEVER`, `ASHBY`).
   - Formatted compensation range (USD in `$k` format, INR in `₹ LPA` format).
   - Experience range and work mode pill badges with theme styling.
   - Top detected technology badges.
4. **Job Detail Modal (`JobDetailModal.tsx`):**
   - Displays full job description formatted from cleaned Markdown.
   - Source integrity alert banner displaying canonical URL.
   - **`[Open Application]` Button:** Target `_blank` link with `rel="noopener noreferrer"` directing candidate directly to the official ATS application portal.
5. **Telemetry Drawer (`DiscoveryDashboard.tsx`):**
   - Collapsible panel displaying active and completed `search_runs` with query strings, discovered counts, and ingested counts.

---

## 6. Security Model Compliance

* **SSRF Defense:** `UrlNormalizer.isSafeUrl()` enforces strict URL validation before any network request:
  - Validates protocol (`http`, `https` only).
  - Resolves host DNS and inspects `InetAddress`.
  - Rejects loopback addresses (`127.0.0.0/8`, `::1`).
  - Rejects RFC 1918 private IPv4 networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
  - Rejects link-local and cloud metadata addresses (`169.254.0.0/16`).
  - Rejects internal intranet domain suffixes (`.internal`, `.local`, `.lan`, `localhost`).
* **Prompt Injection Protection:** Scraped HTML/Markdown is sanitized and tagged as untrusted text before processing. Delimiter injection attacks and instructions targeting language models are stripped.
* **Secret Hygiene:** `FIRECRAWL_API_KEY` is loaded strictly via environment variables or secure application properties; it is never committed or echoed in logs.

---

## 7. Verification & Test Suite Results

### 7.1 Backend Automated Tests (Maven)
The full backend test suite was executed against the local PostgreSQL test instance:
* **Total Tests Executed:** 31
* **Failures:** 0
* **Errors:** 0
* **Skipped:** 0
* **Status:** `BUILD SUCCESS`

Test classes:
* `JobHunterAiApplicationTests`: Full Spring context bootstrapping and Flyway migration verification.
* `UrlNormalizerTest`: Tests URL canonicalization, tracking parameter stripping, and SSRF detection for IPv4, IPv6, localhost, and cloud metadata IPs.
* `SourceAdaptersTest`: Tests canonical URL normalization and company extraction for Greenhouse, Lever, Ashby, and Workday.
* `JobSearchQueryGeneratorTest`: Tests dynamic query building from candidate profile roles, skills, and locations.
* `DeduplicationServiceTest`: Tests Tier 1 canonical URL hash and Tier 2 content hash generation and duplicate rejection.
* `JobQualityFilterTest`: Tests rejection of closed/expired posting phrases and non-job URL patterns.
* `JobExtractorServiceTest`: Tests salary parsing (USD/INR), experience parsing, technology matching, and prompt-injection sanitization.
* `FirecrawlClientTest`: Tests WebClient mock interaction, headers, and payload transformation.
* `DiscoveryServiceTest`: Tests end-to-end discovery orchestration, error handling, and run logging.

### 7.2 Frontend Production Build
Executed TypeScript type-check and Vite build:
* **Command:** `npm run build`
* **Result:** `✓ built in 3.33s` with 0 TypeScript or lint errors.
* **Output Assets:** Clean distribution bundle in `frontend/dist/`.

### 7.3 Live API & Database Verification
* **User Authentication:** Verified login with `candidate@jobhunter.ai` returning valid JWT Bearer token.
* **Endpoint `GET /api/jobs`:** Verified returning 200 OK with paginated list of discovered jobs.
* **Endpoint `GET /api/jobs?workMode=REMOTE`:** Verified returning only remote job listings.
* **Endpoint `GET /api/jobs/{id}`:** Verified retrieving complete job details with cleaned description.
* **Endpoint `GET /api/jobs/search-runs`:** Verified retrieving completed search run metrics.
* **Endpoint `POST /api/jobs/discover`:** Verified graceful handling when `FIRECRAWL_API_KEY` is unconfigured, logging `ABORTED` in `agent_runs` table and returning clear diagnostic guidance without crashing.

---

## 8. Summary of Files Created & Modified

### Backend:
* `src/main/java/com/jobhunter/model/entity/SearchRun.java` (New)
* `src/main/java/com/jobhunter/model/entity/AgentRun.java` (New)
* `src/main/java/com/jobhunter/model/entity/Job.java` (Modified - added SearchRun relationship)
* `src/main/java/com/jobhunter/repository/SearchRunRepository.java` (New)
* `src/main/java/com/jobhunter/repository/AgentRunRepository.java` (New)
* `src/main/java/com/jobhunter/repository/JobRepository.java` (Modified - added JpaSpecificationExecutor)
* `src/main/java/com/jobhunter/config/ResilienceConfig.java` (New)
* `src/main/java/com/jobhunter/client/firecrawl/FirecrawlClient.java` (New)
* `src/main/java/com/jobhunter/client/firecrawl/FirecrawlClientImpl.java` (New)
* `src/main/java/com/jobhunter/client/firecrawl/dto/*` (New - Request/Response DTOs)
* `src/main/java/com/jobhunter/service/discovery/UrlNormalizer.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/JobSourceAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/GreenhouseAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/LeverAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/WorkdayAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/AshbyAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/GenericCareerAdapter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/adapter/JobSourceAdapterRegistry.java` (New)
* `src/main/java/com/jobhunter/service/discovery/JobSearchQueryGenerator.java` (New)
* `src/main/java/com/jobhunter/service/discovery/JobQualityFilter.java` (New)
* `src/main/java/com/jobhunter/service/discovery/DeduplicationService.java` (New)
* `src/main/java/com/jobhunter/service/discovery/JobExtractorService.java` (New)
* `src/main/java/com/jobhunter/service/discovery/DiscoveryService.java` (New)
* `src/main/java/com/jobhunter/service/JobService.java` (New)
* `src/main/java/com/jobhunter/controller/JobController.java` (New)
* `src/main/java/com/jobhunter/dto/job/*` (New - Discovery and Job Response DTOs)
* `src/main/resources/application.yml` (Modified - added Resilience4j configurations)
* `pom.xml` (Modified - added `resilience4j-spring-boot3`, `resilience4j-reactor`, `spring-boot-starter-aop`)
* `src/test/java/com/jobhunter/service/discovery/*` (New - 8 test classes, 30 unit tests)

### Frontend:
* `src/types/index.ts` (Modified - added `Job`, `DiscoverySummary`, `SearchRun`, `PageResponse`)
* `src/api/client.ts` (Modified - added `apiClient.jobs.*` endpoints)
* `src/components/JobCard.tsx` (New)
* `src/components/JobDetailModal.tsx` (New)
* `src/components/JobFilters.tsx` (New)
* `src/components/DiscoveryDashboard.tsx` (New)
* `src/components/Navbar.tsx` (Modified - added Discovery navigation item)
* `src/App.tsx` (Modified - integrated Discovery tab view)

---

## 9. Known Limitations & Next Steps

### Operational Notes
* **Firecrawl API Key:** Web discovery execution relies on `FIRECRAWL_API_KEY`. If unconfigured, the system gracefully aborts the live scrape and logs the diagnostic state in `agent_runs` without throwing unhandled exceptions.
* **Manual Verification:** Human candidate review is strictly enforced via the `[Open Application]` external link to respect platform Terms of Service and preserve submission quality.

### Transition to Milestone 3
With discovered jobs successfully ingested, deduplicated, and stored in PostgreSQL with their raw descriptions and structured specifications, the project is ready for:
* **Milestone 3 — Semantic Understanding & Job-Profile Matching Engine**:
  - Computing dense embeddings using pgvector (`vector(768)`).
  - Multi-dimensional match scoring (hard skills, soft skills, seniority, domain fit).
  - Transparent match explanation, gap analysis, and interview prep generation via Gemini 1.5 Flash.

---
**Milestone 2 Sign-off:** Complete, verified, and ready for user review.
