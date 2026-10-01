# JobHunter AI — Product Specification

## 1. Executive Summary & Vision

### 1.1 Product Mission
JobHunter AI is an **Intelligent Job Search + Application Optimization Platform** designed for software engineers. The platform answers one central question:

> *"Which jobs on the internet are worth my attention, why am I actually relevant to them, and how should I position my real experience when applying?"*

JobHunter AI is **not a bulk auto-submitter**, **not an ATS tracker/CRM**, and **NOT an interview preparation platform**. It is a precision discovery and application copilot that finds high-conviction opportunities across the web, extracts what employers actually care about, matches and audits candidate evidence, guides the candidate in positioning their existing background for maximum application impact, and helps them apply.

### 1.2 Strict Product Scope & Out-of-Scope Boundaries

The product must remain strictly and exclusively focused on:
**Intelligent Job Search + Application Optimization**.

The following capabilities are permanently **OUT OF SCOPE**:
* **Interview preparation**: Technical questions, DSA, system design prep, behavioral coaching, interview roadmaps, revision checklists.
* **Application tracking lifecycle**: Multistage pipelines, interview scheduling, rejection tracking, offer tracking, status stages (`SHORTLISTED`, `INTERVIEW`, `OFFER`, `REJECTED`).
* **Application outcome analytics**: Conversion-rate charts, response-rate analysis, source performance charts, outcome dashboards.
* **Outcome-learning loops**: The system does NOT learn from rejections, interviews, or offers. Intelligence is derived directly from Candidate Profile + Master Resume + Projects + Job Description + Web Discovery + Semantic Matching.

### 1.3 Single Application State: "Have I Already Applied?"
The system only tracks one simple question per job:
**`applied: boolean`** (and optional **`appliedAt: timestamp`**).

The user manually controls this state:
* `[ OPEN APPLICATION PORTAL ]`
* `[ MARK AS APPLIED ]` → `✓ APPLIED`
* `[ MARK AS NOT APPLIED ]` (if clicked accidentally)

### 1.4 Core Philosophy
1. **Depth over Volume**: Applying to 10 deeply understood, well-tailored roles with genuine qualifications yields a dramatically higher callback rate than blast-applying to 500 mismatched listings.
2. **Absolute Factual Truthfulness (Zero-Tolerance Hallucination)**: The system never hallucinates, fabricates metrics, invents technologies, or claims non-existent experience. All tailored materials, cover letters, and application positioning are strictly rooted in the candidate's verified profile via a deterministic Grounding Verification Gate.
3. **Semantic Understanding over Keyword Matching**: Modern job descriptions use diverse terminology. JobHunter AI understands that "experience with relational stores and high-throughput RESTful services" semantically aligns with Spring Boot, PostgreSQL, and Spring Data JPA.
4. **Mandatory Human-in-the-Loop Approval**: The platform automates research, drafting, and portal navigation, but **never silently submits an application in the background**. Every submission requires explicit candidate review and manual submission through the employer's official portal.

---

## 2. Candidate Profile & Target Market

### 2.1 Baseline Candidate Profile
While fully editable and extensible via the application UI, the initial system is pre-configured and optimized around the following candidate profile:

* **Current Role / Experience Tier**: Software Engineer / Associate Software Engineer (1-3 years commercial experience).
* **Primary Technical Core**:
  * **Languages & Runtimes**: Java (11/17+), Node.js (v18+), TypeScript/JavaScript, C# (foundational).
  * **Frameworks & Ecosystem**: Spring Boot 3, Express, React, Tailwind CSS, Hibernate / JPA.
  * **Databases & Stores**: PostgreSQL, MySQL, Redis, DynamoDB (basic).
  * **Cloud, DevOps & Tools**: Docker, AWS (S3, ECS, RDS), Git, GitHub Actions, Linux.
* **Target Roles**: Software Engineer, Backend Engineer, Java Developer, Full Stack Engineer.
* **Location Preferences**: Bangalore (preferred), Hyderabad, Pune, Mumbai, Gurgaon, Delhi NCR, Remote (India / Worldwide).
* **Work Modes**: Remote, Hybrid, On-site.
* **Target Compensation Range**: ₹10,00,000 – ₹25,00,000 INR (10–25 LPA) or $70,000 – $130,000 USD (for global remote).

---

## 3. The End-to-End Product Pipeline

JobHunter AI operates along a linear, 9-step focused product loop:

```
[1. DISCOVER]   ────────► Autonomous Firecrawl search across ATS portals & career sites
      │
      ▼
[2. FILTER]     ────────► Deduplicate, remove expired, filter incompatible seniorities
      │
      ▼
[3. UNDERSTAND] ────────► Extract must-haves, nice-to-haves, stack, responsibilities
      │
      ▼
[4. MATCH]      ────────► Compare against candidate commercial experience & projects
      │
      ▼
[5. PRIORITIZE] ────────► Curate high-conviction roles worth candidate attention
      │
      ▼
[6. THE EDGE]   ────────► Employer priorities, candidate relevance, strongest evidence, positioning
      │
      ▼
[7. TAILOR]     ────────► Grounded bullet sharpening, section reordering, LaTeX/PDF generation
      │
      ▼
[8. APPLY]      ────────► Launch official employer application portal in new tab
      │
      ▼
[9. MARK AS APPLIED] ───► User clicks [ MARK AS APPLIED ] (applied: true). DONE.
```

### Detailed Pipeline Definitions:
1. **Discover**: Find relevant jobs across targeted career pages and ATS portals (Greenhouse, Lever, Ashby, Workday) using multi-variable candidate queries.
2. **Filter**: Remove irrelevant listings, cross-posted duplicates, expired jobs, and incompatible locations/seniority.
3. **Understand**: Deep extraction of must-have skills, preferred skills, responsibilities, seniority, domain, work mode, compensation, and implied requirements.
4. **Match**: Multidimensional comparison against candidate commercial experience, portfolio projects, verified skills, and work preferences.
5. **Prioritize**: Curate high-conviction roles to separate "jobs I could apply to" from "jobs worth my time."
6. **The Edge**: Explain employer priorities, candidate relevance, strongest evidence, what to emphasize/de-emphasize, honest gaps, and application positioning.
7. **Tailor**: Reorder sections and sharpen bullet points to emphasize relevant projects and JD terminology without fabricating claims. Deterministic LaTeX and PDF generation.
8. **Apply**: Prepare application fields and launch the employer's official application portal for human-controlled submission.
9. **Mark as Applied**: The candidate marks the job as applied (`applied: true`, `appliedAt: timestamp`). The system persists this state to avoid duplicate applications.

---

## 4. Key Functional Capabilities

### 4.1 Web Intelligence via Firecrawl
* Integration with the Firecrawl API (`/v1/search`, `/v1/scrape`, `/v1/crawl`, `/v1/map`).
* Targeted multi-variable queries combining role, skills, seniority, location, and work mode.
* Automated canonical URL extraction and normalization (stripping tracking parameters `utm_*`, `gh_src`, etc.).
* Two-tier deduplication via SHA-256 canonical URL hashing and content hashing.
* Detection of stale or expired listings.

### 4.2 Intelligent Job Ranking, Prioritization & Discovery Quality
* **Purpose**: Determines which jobs discovered from the internet are genuinely worth the candidate's attention and explains why ("Why should this candidate spend time on this job?").
* **Distinction between Match and Priority**:
  * **MATCH** answers: *"How well does the candidate's background correspond to this job?"*
  * **PRIORITY** answers: *"Given the match, job quality, freshness, requirements, and constraints, how much attention should this job receive?"*
* **Deterministic Priority Categories**:
  * **HIGH_PRIORITY**: Exceptional fit (>=75% match, strong must-have coverage, fresh posting, zero hard constraint violations). Primary focus for candidate applications.
  * **MEDIUM_PRIORITY**: Solid potential match (50-74% match, or fresh match with minor gaps, or high-match stale listing downranked due to age). Worth tailoring and applying.
  * **LOW_PRIORITY**: Marginal alignment (<50% match, substantial gaps, non-fatal seniority delta, or missing secondary stack). Secondary review.
  * **NOT_RECOMMENDED**: Hard constraint violations (seniority gap >= 2.5 yrs, executive title barrier, missing mandatory domain tech like Swift for iOS, or strict on-site location mismatch). Capped to protect candidate time.
* **Job Freshness Rigor** (Scrape date is strictly NOT posting date):
  * **NEW**: Posted <= 7 days ago.
  * **RECENT**: Posted 8–30 days ago.
  * **OLDER**: Posted 31–60 days ago.
  * **STALE**: Posted > 60 days ago. Automatically downranked to `MEDIUM_PRIORITY` or below due to advanced employer pipeline risk.
  * **UNKNOWN**: Source did not provide a verifiable posting date. Never fabricated or defaulted to today's date.
* **Explanation Transparency**:
  * **Why This Job?**: Direct evidence-backed positives (verified commercial tech alignment, matched seniority, remote/location fit, disclosed salary threshold match).
  * **Potential Concerns**: Transparent negative evidence (identified skill gaps, project-only exposures, stale listing warnings, hard constraint violations).
  * **Key Technologies**: Top 5 critical stack items on JobCard with visual coverage indicators (`✓` Strong commercial, `△` Partial project, `✕` Gap).
* **Hard Constraints Override Soft Signals**: Strict non-negotiable filters cap priority regardless of vector cosine similarity.
* **No Hiring Probability Estimation**: System ranks by candidate-job fit, never predicting employer hiring decisions or offer likelihood.
* **Independence of Applied State**: Prioritization is strictly invariant to `applied` boolean state.
* **3-Tier Search Strategy Generation**:
  * **EXACT**: Target roles + primary core skills + preferred locations across Greenhouse, Lever, Ashby, Workday.
  * **ADJACENT**: Adjacent titles (e.g. Platform Engineer for Backend Engineer) + core stack.
  * **SKILL_LED**: High-signal technology combinations directly targeting career boards.
* **Three-Tier Deduplication**:
  * **Tier 1**: Canonical URL hash (SHA-256).
  * **Tier 2**: Normalized title + company + description content hash.
  * **Tier 3**: Cross-source opportunity deduplication with Jaccard text similarity (>75%), strictly preserving distinct geographic office locations.

### 4.3 Candidate-Job Match Explainer
* Transparent, evidence-backed breakdown:
  * **Strong Matches**: Direct verified commercial experience satisfying requirement.
  * **Partial Matches**: Project/practical exposure without full commercial track record.
  * **Honest Gaps**: Clearly flagged unmet requirements with zero fabrication.
  * **Transferable Skills**: Justified adjacent competence (e.g., PostgreSQL depth transferring to MySQL).

### 4.4 The Edge — Application Advantage Report
* Analyzes the employer's unwritten hiring priorities:
  * Primary candidate advantages
  * Relevant verified projects and commercial achievements
  * Strategic talking points and framing for honest gaps
  * Positioning recommendations

### 4.5 Truthful Resume Tailoring
* Aligns candidate's existing real experience with the specific job description:
  * Preserves original Master Resume immutability.
  * Reorders sections and emphasizes proven skills.
  * Sharpens bullets using JD terminology only when backed by candidate evidence.
  * Rejects unsupported keywords via Grounding Verification Gate.
  * Generates ATS-friendly single-column output (Markdown, LaTeX, PDF).

### 4.6 Application Assistance & Single Application State
* Directly opens the official employer portal (`[ Open Application Portal ]`).
* Single applied state toggle:
  * `[ Mark as Applied ]` → sets `applied = true` and records `appliedAt`.
  * `[ Mark as Not Applied ]` → resets `applied = false` if clicked by accident.
* Discovered job history is retained solely to prevent re-scraping duplicates and persist applied status.

---

## 5. Non-Functional Requirements & Performance Goals

| Category | Requirement | Target Metric |
| :--- | :--- | :--- |
| **Grounding Integrity** | Factual truthfulness in application positioning | 100% (Zero hallucinated metrics, roles, or skills) |
| **Discovery Latency** | Full JD scraping and structured analysis | < 8 seconds per job (cached: < 100ms) |
| **System Availability** | Core API and web application availability | 99.5% uptime |
| **Deduplication** | Cross-posting deduplication (One Job = One Opportunity) | > 98% accuracy across ATS & direct boards |
| **User Agency** | Application submission autonomy | 100% human-approved (0% autonomous background submissions) |

---

## 6. Success Metrics & Key Performance Indicators (KPIs)
1. **Application Relevance**: > 80% of prioritized jobs match candidate verified skills and career preferences.
2. **Search-to-Application Efficiency**: Reduce time from job discovery to fully tailored application submission to under 5 minutes.
3. **Zero Hallucination Compliance**: 100% compliance with verified candidate ground truth across all generated collateral.
