# PROFILE-FIRST DISCOVERY ARCHITECTURE
## Intelligent Job Search & Application Optimization

**Document Version:** 1.0.0  
**Status:** Canonical Architecture Specification  
**Core Principle:** *Understand the candidate first. Search broadly second. Filter aggressively third. Show only jobs that are genuinely useful to the candidate.*

---

## 1. Executive Summary & Core Philosophy

JobHunter AI is **not** a generic job board, a database browser, or a recruiter CRM. In conventional job portals (LinkedIn, Indeed, Naukri), candidates are exposed to thousands of noisy, uncurated listings, forced to manually sift through misleading titles, hidden location requirements, mismatched experience criteria, and keyword-stuffed job descriptions.

JobHunter inverts this paradigm:
1. **Candidate Truth Baseline:** No discovery operation or job ranking ever occurs in a vacuum. The candidate's verified profile and canonical fact store constitute the sole foundation for all discovery actions.
2. **Search Broadly, Display Narrowly:** The system autonomously crawls and scrapes hundreds of opportunities across ATS systems (Greenhouse, Lever, Ashby, Workday, company career portals, job boards).
3. **Aggressive Multi-Tier Filtering:** Before a single job touches the candidate's screen, it must survive:
   - **Hard Eligibility Filtering** (eliminating structural disqualifiers like massive experience gaps, visa/work authorization walls, or geographical dead ends).
   - **Multi-Dimensional Semantic Profile Matching** (evaluating verified candidate evidence against extracted requirements across role, technology, responsibility, domain, and seniority).
   - **Usefulness Gate** (demanding genuine candidate relevance, discarding weak matches that merely fill feed space).
4. **Absolute Quality Invariant:** If only 8 jobs across the internet are truly useful for a candidate, JobHunter displays **8 jobs**. It will never artificially dilute the feed with 50 mediocre postings to simulate volume.

```
                    ┌────────────────────────────────────────┐
                    │     CANDIDATE FACT BASELINE            │
                    │  (Resume, Verified Exp, Skills, Edu)   │
                    └───────────────────┬────────────────────┘
                                        │
                         [ ProfileReadinessService ]
                                        │
                       ┌────────────────┴────────────────┐
                       ▼                                 ▼
             [ PROFILE_NOT_READY ]               [ PROFILE_READY ]
             Discovery Locked ⛔                  Discovery Unlocked 🚀
             Checklist UI displayed                            │
                                                               ▼
                                                  [ Search Strategy Generator ]
                                                  Dynamic Profile Query Families
                                                               │
                                                               ▼
                                                  [ Web Discovery (Firecrawl) ]
                                                  1,000+ Raw Opportunities
                                                               │
                                                               ▼
                                                  [ Hard Eligibility Filter ]
                                                  Obvious Mismatches Dropped
                                                               │
                                                               ▼
                                                  [ Semantic Profile Matching ]
                                                  Evidence vs Requirements
                                                               │
                                                               ▼
                                                  [ Usefulness Gate ]
                                                  Strict usefulnessStatus == USEFUL
                                                               │
                                                               ▼
                                                  [ Multi-Tier Deduplication ]
                                                  Canonical URL & Content Hashes
                                                               │
                                                               ▼
                                                  [ Candidate Results Feed ]
                                                  "24 roles match your profile"
```

---

## 2. Profile Readiness Engine (`ProfileReadinessService`)

Discovery must never precede profile completeness. A candidate cannot execute web discovery or browse opportunities until JobHunter understands who they are.

### 2.1 Readiness Dimensions & Evaluation Criteria

| Dimension | Minimum Required Threshold | Weight | Mandatory? |
|---|---|---|---|
| **Master Resume** | Uploaded, parsed text extracted ($\ge 100$ characters) | 20% | **Yes** |
| **Verified Experience** | At least 1 commercial/professional experience record | 20% | **Yes** |
| **Verified Skills** | At least 3 verified technical/domain skills | 15% | **Yes** |
| **Education** | Degree or institution verified in profile or master resume | 10% | **Yes** |
| **Target Roles** | At least 1 target role specified in candidate preferences | 15% | **Yes** |
| **Location Preference** | At least 1 preferred location or "Remote" | 10% | **Yes** |
| **Work Mode Preference** | At least 1 work mode (REMOTE, HYBRID, ONSITE) | 10% | **Yes** |
| **Salary Baseline (Optional)** | Minimum CTC expectation specified | Optional (+5% bonus) | No |
| **Social / Portfolio Links** | GitHub, LinkedIn, or Portfolio URL | Optional (+5% bonus) | No |

### 2.2 Readiness States & Score

$$\text{Readiness Score} = \sum_{\text{criterion} \in \text{Mandatory}} \text{Score}(\text{criterion})$$

- **`PROFILE_READY`**: Score $\ge 85\%$ **AND** all mandatory criteria satisfied.
- **`PROFILE_NOT_READY`**: Score $< 85\%$ **OR** any mandatory criterion missing.

### 2.3 Readiness Data Contract

```json
{
  "state": "PROFILE_READY",
  "score": 100,
  "canDiscover": true,
  "headline": "Profile Ready for Intelligent Discovery",
  "message": "JobHunter understands enough about you to start finding relevant opportunities.",
  "items": [
    { "key": "resume", "label": "Master Resume", "satisfied": true, "details": "Master resume parsed (2,410 chars)" },
    { "key": "experience", "label": "Commercial Experience", "satisfied": true, "details": "1 position recorded (2.5 years)" },
    { "key": "skills", "label": "Verified Technical Skills", "satisfied": true, "details": "7 verified technical skills" },
    { "key": "education", "label": "Education Background", "satisfied": true, "details": "Bachelor of Technology recorded" },
    { "key": "targetRoles", "label": "Target Roles", "satisfied": true, "details": "3 target roles defined" },
    { "key": "locations", "label": "Location Preference", "satisfied": true, "details": "Bangalore, Remote India" },
    { "key": "workModes", "label": "Work Mode Preference", "satisfied": true, "details": "REMOTE, HYBRID" }
  ],
  "missingItems": []
}
```

---

## 3. Search Strategy & Dynamic Query Generation

JobHunter never executes blind, ungrounded queries like `"software engineer jobs"`. It generates multi-dimensional search query families synthesized directly from the candidate profile:

### 3.1 Dimensions of Query Variation

1. **Role Terminology:** Translating candidate target roles into industry synonyms (e.g., "Backend Engineer", "Java Developer", "Software Development Engineer II", "Server-Side Engineer").
2. **Technology Terminology:** Anchoring queries around candidate core verified stack (e.g., "Java Spring Boot", "PostgreSQL REST API").
3. **Location & Geo Framing:** Combining target cities with country and remote modifiers (e.g., "Bangalore", "India Remote", "Hybrid Bangalore").
4. **Seniority Bracketing:** Mapping candidate years of experience ($2.5\text{ yrs}$) to exact seniority tiers ("Mid-Level", "2-4 years", "Software Engineer II") while strictly omitting out-of-scope tiers ("Staff", "Director", "VP", "Principal").

### 3.2 Query Family Generation Algorithm

```
Query Family 1: [Target Role] + [Primary Tech] + [Primary Location]
  -> "Backend Engineer Java Spring Boot Bangalore"
Query Family 2: [Target Role] + [Secondary Tech] + [Work Mode]
  -> "Java Software Engineer PostgreSQL Remote India"
Query Family 3: [ATS Platform Direct Search]
  -> "site:boards.greenhouse.io \"Software Engineer\" \"Java\" \"Spring Boot\""
Query Family 4: [Specialized High-Alignment Search]
  -> "site:jobs.lever.co \"Backend Engineer\" \"Spring Boot\" India"
```

---

## 4. Hard Eligibility Filter (`HardEligibilityFilter`)

Executed immediately following document scraping and structured job extraction. This eliminates structurally incompatible roles **before** running expensive LLM/vector semantic analysis.

### 4.1 Strict Disqualification Rules

1. **Severe Experience Delta:**
   - If Job minimum required experience $> \text{Candidate YoE} + 3.0$ years (e.g., candidate has 2.5 yrs, job demands 7+ or 8+ yrs) $\implies$ **REJECT**.
   - If Job explicitly requires "Staff Engineer", "Principal Engineer", "VP", or "Director" and candidate YoE $< 6.0$ years $\implies$ **REJECT**.
2. **Geographical / Remote Incompatibility:**
   - If Job is strictly On-Site in a foreign country (e.g., "On-site in New York / London") and candidate has no international relocation eligibility $\implies$ **REJECT**.
3. **Domain / Role Disconnect:**
   - If normalized title or extracted core responsibility is entirely outside engineering (e.g., "Product Designer", "Sales Account Executive", "Nurse", "Graphic Artist", "Legal Counsel") $\implies$ **REJECT**.
4. **Hard Education Gates:**
   - If Job states "PhD required" or "Doctorate mandatory" and candidate does not possess a PhD $\implies$ **REJECT**.

### 4.2 Non-Aggressive Invariance (What Hard Filter NEVER Rejects)

- Never reject because a preferred or optional technology is missing (e.g., job mentions Kafka as nice-to-have).
- Never reject because candidate experience is slightly below preferred (e.g., candidate has 2.5 yrs, job asks for 3 yrs).
- Never reject due to synonymous terminology (e.g., "RESTful web services" vs "API development").

---

## 5. Semantic Profile Matching & Evidence Grounding

Jobs surviving the hard filter proceed to multi-factor semantic evaluation:

1. **Structured Requirement Extraction:** Splitting JD into MUST-HAVE vs PREFERRED requirements across categories (TECH, EXPERIENCE, RESPONSIBILITY, DOMAIN, LOCATION, EDUCATION).
2. **Vector Cosine Similarity:** Evaluating dense embedding similarity between candidate summary/experience and job description.
3. **Evidence Alignment:** Every requirement is checked against `CandidateFactStoreService`.
   - `STRONG`: Directly supported by verified commercial experience or skill.
   - `PARTIAL`: Supported by candidate project or adjacent technology.
   - `GAP`: Not supported in verified candidate evidence.

---

## 6. The Usefulness Gate (`UsefulnessGate`)

The critical filter that decides whether an analyzed job earns the right to be displayed to the candidate.

### 6.1 Usefulness Decision Matrix

An analyzed job is classified into one of three usefulness categories:

| Category | Eligibility Criteria | Usefulness Status | Surface to User? |
|---|---|---|---|
| **`HIGH_RELEVANCE`** | Composite score $\ge 68\%$, at least 2 strong matches, zero critical must-have violations, strong technology & role alignment. | `USEFUL` | **Yes (Top Tier)** |
| **`GOOD_MATCH`** | Composite score $48\% - 67\%$, at least 1 strong match, role & core stack aligned, minor gaps only in preferred/optional skills. | `USEFUL` | **Yes** |
| **`POSSIBLE_MATCH`** | Composite score $38\% - 47\%$, relevant role and seniority, but notable gaps in secondary requirements. | `USEFUL` (Qualified) | **Yes (If verified relevant)** |
| **`LOW_RELEVANCE`** | Composite score $< 38\%$, or zero strong matches, or critical must-have unmet. | `NOT_USEFUL` | **No (Filtered Out)** |
| **`DO_NOT_APPLY`** | Severe seniority mismatch, incompatible location, or hard constraint violated. | `NOT_USEFUL` | **No (Filtered Out)** |

### 6.2 Zero Fake Feed Inflation

If a search discovers 100 jobs and only 12 meet `usefulnessStatus == USEFUL`, the API returns **12 jobs**. The UI displays:
`12 ROLES MATCH YOUR PROFILE`
*(JobHunter reviewed 100+ opportunities to surface these).*

---

## 7. Multi-Tier Deduplication

Discovered jobs are deduplicated across multiple vectors:
1. **Tier 1 (URL Normalization & Hash):** Strips UTM tracking codes, normalizes scheme, and hashes canonical URL.
2. **Tier 2 (Content Digest):** SHA-256 fingerprint of normalized job description markdown.
3. **Tier 3 (Opportunity Identity):** Fuzzy match on `(Company Name + Normalized Title + Location)`. If identical posting appears on Greenhouse, Lever, and LinkedIn, it is consolidated into a single opportunity specimen, retaining the highest-quality direct application URL.

---

## 8. Backend API & Enforcement Architecture

### 8.1 Endpoint Security & Readiness Enforcement

- **`GET /api/candidate-profile/readiness`**
  - Returns current `ProfileReadinessReport` with completeness percentage and checklist.
- **`GET /api/jobs`**
  - **Precondition Check:** Evaluates `profileReadinessService.evaluateReadiness(userId)`.
  - If `!report.isCanDiscover()`: Throws `ProfileIncompleteException` / Returns HTTP 428 Precondition Required with code `PROFILE_REQUIRED` and the readiness breakdown.
  - **Filtering:** Filters exclusively for jobs that pass the Hard Eligibility Filter and Usefulness Gate (`usefulnessStatus == USEFUL`).
- **`POST /api/jobs/discover`**
  - **Precondition Check:** Evaluates `profileReadinessService.evaluateReadiness(userId)`.
  - If `!report.isCanDiscover()`: Aborts immediately with `PROFILE_REQUIRED`.
  - Executes dynamic query generation from authenticated user profile and runs Firecrawl discovery.

### 8.2 Security Invariant
Candidate profile data is **never** accepted from request bodies for discovery or matching operations. The backend always loads the authoritative profile directly from the PostgreSQL database using `userDetails.getId()`.

---

## 9. Frontend User Experience & UI Flow

### 9.1 Profile Incomplete State
When a user visits the discovery interface with an incomplete profile:
- Discovery feed and search inputs are hidden.
- The user is greeted with the cinematic technical readiness card:
  - **Title:** "YOUR PROFILE ISN'T READY"
  - **Subtext:** "JobHunter needs to understand you before it can find jobs worth your time."
  - **Visual Progress Bar:** Showing exact readiness percentage.
  - **Requirement Checklist:** Items marked with green checks or amber circles.
  - **Action Button:** `[ COMPLETE PROFILE ]` which switches to the Profile Intelligence editor.

### 9.2 Profile Ready State
Once readiness reaches $\ge 85\%$:
- **Status:** `PROFILE READY // DISCOVERY UNLOCKED`
- Subtitle: "JobHunter understands enough about you to start finding relevant opportunities."
- Opportunity Feed displays:
  - `X ROLES MATCH YOUR PROFILE`
  - Subtext: `Aggressively filtered from hundreds of web opportunities`
  - Badges for `HIGH RELEVANCE`, `GOOD MATCH`, `THE EDGE` advantage.

---

## 10. Failure & Edge States

1. **Zero Useful Jobs Found:**
   - UI does not say "Error". It truthfully explains:
     *"JobHunter evaluated [N] opportunities across the web, but none met our strict usefulness threshold for your profile. Try updating your target roles or running a fresh web crawl."*
2. **Firecrawl API Key Missing:**
   - Gracefully reports that web crawling is offline while retaining existing verified local opportunities.
3. **Profile Edit Synchronization:**
   - Adding a skill or modifying target roles immediately invalidates stale match evaluations and updates subsequent discovery queries.
