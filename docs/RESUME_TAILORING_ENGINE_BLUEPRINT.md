# AI Resume Tailoring Engine: Architecture & Implementation Blueprint

Extracted from: OfferPilot Backend Architecture  
System Status: Production Implementation for JobHunter AI

---

## 1. System Overview & Core Philosophy

The primary failure mode of AI resume tailorers is hallucination: LLMs routinely invent past technologies, exaggerate metrics, fabricate companies, or blindly inject job description keywords into the candidate's skillset.

This engine solves this with a **grounded-by-contract pipeline**:

1. **Master Resume as Single Source of Truth**:
   The candidate's master resume is treated as immutable ground truth. The AI is strictly restricted to selecting, reordering, clarifying, and reframing existing evidence.
2. **Explicit Skill Triaging**:
   The AI is forced to classify every JD skill into `MATCHED`, `PARTIALLY_MATCHED`, or `MISSING` with explicit evidence for each classification.
3. **Deterministic Guardrail Validation**:
   A dedicated programmatic guardrail service verifies the output. If the AI marked a skill as `MISSING` but secretly added it to the resume's skills section, the request fails fast and throws an error.
4. **Automated Fit Assessment**:
   Programmatic heuristics alert the user if critical JD requirements are missing or if the role has a high mismatch ratio.
5. **Typesafe Structured JSON -> Deterministic LaTeX Rendering**:
   The AI never generates LaTeX or Markdown directly. It returns strict JSON, which is rendered deterministically by a dedicated LaTeX compiler with full character escaping to prevent compilation errors.

---

## 2. High-Level Workflow

```
+----------------------------+       +------------------------------------+
| Master Resume (PDF / DOCX) |       | Job Description (Raw Text / URL)   |
+----------------------------+       +------------------------------------+
              |                                         |
              v                                         v
+----------------------------+       +------------------------------------+
| Text Extraction & Cleaning |       | Pre-Extracted JD Intelligence      |
| (Apache Tika / PDF parser) |       | (Skills, Priorities, Requirements) |
+----------------------------+       +------------------------------------+
              \                                         /
               \                                       /
                v                                     v
           +-----------------------------------------------+
           | Prompt Builder (Strict Anti-Hallucination)    |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | LLM Generation (Temp: 0.1, Mode: JSON)        |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | JSON Schema Validation (DTO / Object Mapping) |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | Guardrail Service (Anti-Hallucination Engine) |
           | - Rejects if missing skills are in resume     |
           | - Injects missing required skill warnings     |
           | - Injects fit risk warnings (Missing >= 3)    |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | Database Persistence (Status: GENERATED)      |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | Deterministic LaTeX Renderer & Escaper        |
           +-----------------------------------------------+
                                  |
                                  v
           +-----------------------------------------------+
           | Clean, ATS-Optimized .tex Resume File & PDF   |
           +-----------------------------------------------+
```

---

## 3. Database Schema

```sql
ALTER TABLE tailored_resumes
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS target_job_title VARCHAR(150),
    ADD COLUMN IF NOT EXISTS template_name VARCHAR(80) NOT NULL DEFAULT 'PROFESSIONAL_DEFAULT',
    ADD COLUMN IF NOT EXISTS structured_content_json TEXT,
    ADD COLUMN IF NOT EXISTS matched_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS partially_matched_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS missing_skills_json TEXT,
    ADD COLUMN IF NOT EXISTS tailoring_notes_json TEXT,
    ADD COLUMN IF NOT EXISTS latex_file_path VARCHAR(1000);
```

---

## 4. Input Pipeline: Text Extraction & Sanitization

1. Strip null bytes: `replace("\u0000", "")`
2. Replace horizontal/vertical tabs and form feeds with a single space: `replaceAll("[\\t\\x0B\\f\\r]+", " ")`
3. Collapse consecutive spaces into a single space: `replaceAll(" +", " ")`
4. Collapse excessive newlines (3 or more) into double newlines: `replaceAll("\\n{3,}", "\n\n")`
5. Validation: Ensure extracted text length is at least 100 characters. If lower, throw `IllegalArgumentException("Could not extract readable text from resume")`.

---

## 5. Anti-Hallucination Prompt Specification

The prompt enforces strict behavioral boundaries:
- 14 critical anti-hallucination rules.
- Explicit triaging of all JD skills into `matchedSkills`, `partiallyMatchedSkills`, and `missingSkills`.
- Ban on injecting `missingSkills` into the tailored resume skills.
- Low temperature (0.1) and strict `application/json` mode.

---

## 6. Guardrail & Verification Service

- Normalization regex: `value.trim().toLowerCase().replaceAll("[^a-z0-9+#. ]", "").replaceAll("\\s+", " ")`.
- Missing Skill Leak Detection: Fails fast if any missing skill appears in tailored skills.
- Programmatic Note Augmentation:
  - Missing skills disclosure.
  - Missing required skills warning.
  - Fit warning heuristic ($M \ge 3$ and $M \ge S$).

---

## 7. Deterministic LaTeX Rendering & Character Escaping

- Two-stage escaping: Typography normalization followed by TeX control character escaping.
- Clean, standard LaTeX template compiling ATS-friendly resumes without runtime formatting errors.

---

## 8. Closed-Loop ATS Scoring

- Baseline score of Master Resume vs JD.
- Post-tailoring score of Tailored Resume vs JD.
- Comparison delta with newly aligned keywords and gap analysis.
