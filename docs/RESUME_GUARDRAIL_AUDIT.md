# RESUME GUARDRAIL AUDIT & INTEGRITY REDESIGN

**Document Version:** 1.0.0  
**Status:** AUDITED & ARCHITECTURE APPROVED  
**Date:** October 1, 2026  
**System:** JobHunter AI — Resume Tailoring Pipeline

---

## EXECUTIVE SUMMARY

A critical integrity failure was observed in the JobHunter AI Resume Tailoring system: generated tailored resumes were capable of inventing education degrees, employers, unverified skills, ungrounded technical metrics, and fabricated achievements.

This document presents a comprehensive forensic audit of the end-to-end resume tailoring pipeline, identifying the exact ingress points for hallucinated or fabricated data, analyzing why previous grounding defenses were insufficient, and defining an unbypassable, two-stage verified architecture centered on a **Canonical Candidate Fact Store** and an autonomous **ResumeClaimAuditor** with fail-closed semantics.

---

## 1. CURRENT DATA FLOW

```mermaid
flowchart TD
    A[Master Resume PDF / Profile] --> B[ResumeParserService / ResumeAiOrchestratorService]
    B --> C[CandidateProfile, CandidateSkill, CandidateExperience, CandidateProject]
    C --> D[JobRequirementExtractor & Job Entity]
    D --> E[SemanticMatchingService]
    E --> F[MatchAnalysisResponse & The Edge]
    F --> G[ResumeTailoringService.generateTailoringPlan]
    G --> H[TailoringPlanDto - Bullet Proposals & Section Ordering]
    H --> I[ResumeTailoringService.tailorResume]
    I --> J[buildTailoredMarkdown]
    I --> K[PdfGenerationService.generateLatexSource]
    I --> L[PdfGenerationService.generatePdfDocument / renderPdfWithPdfBox]
    L --> M[PdfGenerationService.validatePdf]
    M --> N[TailoredResume Entity Stored & Export /api/tailored-resumes/{id}/download]
```

### Flow Breakdown:
1. **Candidate Profile & Ingestion:**
   - Candidate uploads a master resume or enters profile details.
   - Text is parsed into `CandidateProfile`, `CandidateSkill`, `CandidateExperience`, and `CandidateProject`.
   - *Gap:* No dedicated `CandidateEducation` entity existed; education was largely omitted from structured entities or hardcoded downstream.
2. **Matching & Requirement Extraction:**
   - Target job requirements are parsed into `JobRequirement` entities.
   - `SemanticMatchingService` calculates semantic similarity and runs `GroundingVerificationGate.verifyTechnicalClaim` against candidate evidence.
3. **Tailoring Decision Generation:**
   - `ResumeTailoringService.generateTailoringPlan` extracts emphasized skills, de-emphasized skills, and bullet sharpening proposals.
4. **Markdown & LaTeX Compilation:**
   - `buildTailoredMarkdown` and `generateLatexSource` synthesize text documents from the profile, plan, and experiences.
5. **PDF Rendering & Validation:**
   - `PdfGenerationService` renders LaTeX via `pdflatex` or falls back to Apache PDFBox 3.x (`renderPdfWithPdfBox`).
   - `validatePdf` inspects file size, page count, readable character count, candidate name presence, standard ATS section headers, and template placeholder syntax (`{{`).
6. **Download / Delivery:**
   - The user requests `/api/tailored-resumes/{id}/download` which serves the generated binary PDF.

---

## 2. CURRENT GUARDRAILS

Prior to this audit, the system had three localized guardrail mechanisms:

1. **`GroundingVerificationGate.verifyTechnicalClaim`:**
   - Purpose: Checks whether a technical claim in `RequirementMatchItem` exists in candidate profile technologies.
   - Demotes claims asserting "commercial" experience to "project/academic" if not in commercial employment.
   - Scope: Only ran during *matching analysis* and for populating `plan.getRejectedKeywords()`. Did **not** inspect or gate the actual resume output text.
2. **`auditAtsKeywordsAndRejections`:**
   - Purpose: Checked JD keywords against candidate verified technologies.
   - Appended unsupported terms to `rejectedKeywords` in `TailoringPlanDto`.
   - Scope: Advisory only; did not enforce an allowlist on what could be written in experience bullets or summaries.
3. **`PdfGenerationService.validatePdf`:**
   - Purpose: Technical format sanity check.
   - Verified that the PDF had `> 0` bytes, `pageCount >= 1`, non-empty text, candidate name string, ATS headers, and no unreplaced `{{...}}` tokens.
   - Scope: Purely structural; zero semantic or factual validation.

---

## 3. WHERE UNSUPPORTED INFORMATION ENTERS (INGRESS POINTS)

The audit identified **six critical ingress points** where fabricated information entered:

### Ingress Point A: Hardcoded Education Hallucination
- **Location:** `ResumeTailoringService.java` (lines 628–630) and `PdfGenerationService.java` (lines 385–392, 696–699)
- **Defect:** 
  ```java
  // In ResumeTailoringService:
  sb.append("## Education\n");
  sb.append("- **Bachelor of Technology in Computer Science & Engineering** (2019 - 2023)\n");

  // In PdfGenerationService:
  return "\\textbf{Bachelor of Technology in Computer Science \\& Engineering} \\hfill 2019 -- 2023 \\\\\n" +
         "\\textit{Affiliated University of Technology} \\hfill First Class with Distinction\n";
  ```
- **Impact:** Any candidate (e.g., self-taught engineer, physics major, or B.Tech ECE graduate) was assigned a fabricated Computer Science degree from a fictional university.

### Ingress Point B: Synthetic Metrics and Scope Escalation in Bullet Proposals
- **Location:** `ResumeTailoringService.java` (lines 408–436, 471–480)
- **Defect:** Template strings injected unverified metrics and technologies:
  - *"slashing batch reporting latency from 14s to 1.8s"*
  - *"optimizing PostgreSQL queries for sub-50ms latency"*
  - *"Architected distributed payment idempotency mechanism preventing duplicate transactions across financial services"*
  - *"Java 17"*
- **Impact:** Fabricated performance percentages, latency figures, and architecture claims that never occurred in the candidate's actual history.

### Ingress Point C: Summary Statement Over-Claiming
- **Location:** `ResumeTailoringService.java` (lines 544–552) and `PdfGenerationService.java` (lines 684–694)
- **Defect:**
  ```java
  return "Software Engineer with " + profile.getYearsOfExperience() + " years of verified commercial production experience specializing in "
          + String.join(", ", strongSkills)
          + ". Proven track record in high-reliability RESTful API development, PostgreSQL database performance tuning, and resilient microservice architectures.";
  ```
- **Impact:** Candidates with no database or microservice experience were described as possessing "proven track records" in PostgreSQL performance tuning.

### Ingress Point D: Fuzzy Matching / Synonym Skill Expansion
- **Location:** `GroundingVerificationGate.java` (lines 166–182)
- **Defect:** `hasFuzzyTechMatch` equated `microservices` with `spring boot` or `rest api`, and `spring` with `spring boot`.
- **Impact:** Technology synonyms caused unverified skills to bleed into the candidate profile without direct evidence.

### Ingress Point E: Job Requirement Bleed into Candidate Facts
- **Location:** Tailoring prompt generation and matching contexts.
- **Defect:** No strongly typed boundary separating `JobFact` (requirements stated by employer) from `CandidateFact` (evidence verified from candidate).
- **Impact:** Risk of model or template treating JD requirements as candidate qualifications.

### Ingress Point F: Post-Generation Validation Black Hole
- **Location:** `PdfGenerationService.validatePdf`
- **Defect:** The validator never verified whether the text inside the generated PDF matched candidate records. It marked any readable PDF containing the candidate's name as `passed=true`, triggering `status="GENERATED"` and allowing immediate download.

---

## 4. WHY THE EXISTING GUARDRAIL FAILED

1. **Gate Placement Was Wrong:** The `GroundingVerificationGate` was placed at the *input* stage (matching analysis), not at the *output* stage (document export). It governed what the candidate matched, not what the PDF compiler wrote.
2. **Deterministic Code Hallucinated:** The hallucination was not solely LLM-driven; template methods in Java services had hardcoded fallback strings containing fictional universities, degrees, and latency metrics.
3. **No Fact Allowlist:** The system lacked a canonical, immutable registry of verified candidate facts against which every output token could be audited.
4. **Pass-Through Format Validator:** `validatePdf` checked PDFBox byte stream health rather than semantic truthfulness.

---

## 5. PROPOSED ARCHITECTURE

To eliminate all fabrication ingress points, the pipeline is restructured into an unbypassable, two-stage verified system:

```mermaid
flowchart TD
    subgraph Candidate Evidence Ingestion
        A[Candidate Profile / Master Resume] --> B[Canonical Candidate Fact Store]
        B --> B1[CandidateFact Entities: EDUCATION, EMPLOYMENT, ROLE, SKILL, METRIC, etc.]
    end

    subgraph Strictly Constrained Tailoring
        B1 --> C[Structured Tailoring Plan]
        C --> D[Constrained Bullet Selector / Rewriter]
        D -->|Only references sourceFactIds| E[TailoredResumeDocument]
    end

    subgraph Two-Stage Integrity Gate
        E --> F[Stage 1: Structured Document Validation]
        F -->|Checks Allowable Orgs, Degrees, Skills, Metrics| G{Passes Stage 1?}
        G -- No --> H[Status: VALIDATION_FAILED - Export Blocked]
        G -- Yes --> I[Deterministic LaTeX & PDF Renderer]
        I --> J[Generated PDF Document]
        J --> K[Stage 2: Rendered Document Claim Auditor]
        K -->|Extracts PDF text, verifies all claims against Fact Store| L{Passes Stage 2?}
        L -- No --> H
        L -- Yes --> M[Status: READY_FOR_DOWNLOAD]
    end

    subgraph Audit & Download
        M --> N[Audit Log Persisted]
        M --> O[/api/tailored-resumes/{id}/download]
        H --> P[User Review: Detailed Failure Report with Unsupported Claims]
    end
```

### Architectural Pillars:

1. **Canonical Candidate Fact Store (`CandidateFact`):**
   - Strongly typed domain model representing all verified atomic candidate facts.
   - Categories: `EDUCATION`, `EMPLOYMENT`, `ROLE`, `SKILL`, `TECHNOLOGY`, `PROJECT`, `CERTIFICATION`, `ACHIEVEMENT`, `RESPONSIBILITY`, `DATE`, `LOCATION`, `METRIC`.
   - Evidence Levels: `VERIFIED`, `SUPPORTED`, `UNKNOWN`. Any fact with `UNKNOWN` is forbidden from appearing in a resume.
2. **Type Separation: `JobFact` vs `CandidateFact`:**
   - Strict compile-time boundary: `JobFact` instances can never be cast or converted into `CandidateFact`.
3. **Structured Resume Model (`TailoredResumeDocument`):**
   - The LLM / generator does **not** emit freeform LaTeX or raw PDF documents.
   - It populates a `TailoredResumeDocument` containing structured sections, where every bullet and statement points to explicit `sourceFactIds`.
4. **Constrained Bullet Rewriting & Metrics Shield:**
   - Rewriting can only use facts provided in the prompt's `allowedFacts`.
   - Output schema requires `usedFacts` and `unsupportedClaims`. If `unsupportedClaims` is not empty, it is instantly rejected.
   - Zero metric injection: No percentage, latency, throughput, or dollar figure may appear in output unless present verbatim in `CandidateFact`.
5. **Strict Organization & Degree Allowlisting:**
   - Extraction of all company, institution, and university names.
   - Compared against `AllowedOrganizations(candidateId)`. If an organization is unrecognized, export is **blocked**.
   - Education is generated strictly from verified records. If no degree exists, the education section is omitted rather than invented.
6. **Resume Claim Auditor (`ResumeClaimAuditor`):**
   - Independent verification component executing two validation stages:
     - **Stage 1 (Structured):** Inspects `TailoredResumeDocument`.
     - **Stage 2 (Rendered):** Extracts raw text from the compiled PDF using PDFBox and verifies zero ungrounded claims, unapproved orgs, or unverified skills.
7. **Strict State Machine & Fail-Closed Semantics:**
   - Lifecycle: `DRAFT` $\to$ `GENERATED` $\to$ `VALIDATING` $\to$ `VALIDATED` $\to$ `PDF_GENERATED` $\to$ `READY_FOR_DOWNLOAD`.
   - Any failure sets `status = VALIDATION_FAILED` and locks the download endpoint.
   - Unknown fact classification $\to$ **BLOCK** (fail closed).

---

## 6. SECURITY & INTEGRITY IMPLICATIONS

1. **Prompt Injection Defense:**
   - External job descriptions are treated strictly as untrusted data.
   - System prompts isolate JD text within delineated data blocks and explicitly declare that JD contents cannot alter candidate facts or override integrity rules.
2. **No User Bypass / "Trust AI" Loophole:**
   - If an unsupported claim is flagged, the user cannot click "Accept Anyway" to bypass the gate.
   - The user must explicitly add verified evidence to their profile before a claim can be incorporated.
3. **Full Auditability:**
   - Every generated document logs `candidateId`, `jobId`, `validatorVersion`, `claimsChecked`, `claimsPassed`, `claimsFailed`, and `validationStatus`.

---

## 7. TEST STRATEGY

| Test Category | Target Behavior | Expected Result |
|---|---|---|
| **Education Guard** | Candidate with B.Tech ECE; target JD requires Master's in CS. | Output contains ONLY B.Tech ECE; zero mention of Master's or CS. |
| **Education Omission** | Candidate with NO education on profile. | Education section is cleanly omitted; zero hallucinated degrees. |
| **Employer Allowlist** | JD mentions "Amazon", candidate worked at "Razorpay". | Generated resume contains "Razorpay"; "Amazon" strictly rejected. |
| **Skill Containment** | Candidate has Java/Postgres; JD requires Kafka. | Kafka rejected from Skills, Experience, and Summary; noted only as gap in analysis. |
| **Metric Preservation** | Candidate has bullet without metrics; prompt/template tries to sharpen. | Sharpened bullet preserves qualitative scope; ZERO invented percentages or ms figures. |
| **Stage 1 Gate** | `TailoredResumeDocument` with unmapped `sourceFactId`. | Validation fails with `UNSUPPORTED_CLAIM`; export blocked. |
| **Stage 2 Gate** | Synthesized PDF containing an unallowlisted university. | Text extraction catches unauthorized organization; status set to `VALIDATION_FAILED`. |
| **Fail-Closed API** | Attempt to call `/api/tailored-resumes/{id}/download` when status != `READY_FOR_DOWNLOAD`. | Returns `403 Forbidden` / `422 Unprocessable Entity` with failure report. |
