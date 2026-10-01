package com.jobhunter.service.tailoring.offerpilot;

import java.util.List;

/**
 * Strict Anti-Hallucination Prompt Builder.
 * Implements Section 5 of the OfferPilot Architecture Blueprint.
 */
public class ResumeTailoringPromptBuilder {

    private static final String PROMPT_TEMPLATE = """
You are a resume tailoring engine for a software engineering career platform.

Your task:
Tailor the candidate's master resume for the target job description.

CRITICAL RULES:
1. The master resume is the only source of truth for candidate facts.
2. Do not invent companies, roles, dates, degrees, certifications, projects,
   metrics, achievements, or technologies.
3. Do not add a skill unless it is explicitly supported by the master resume.
4. If a JD skill is missing from the resume, put it in missingSkills.
5. You may rewrite, reorder, shorten, and clarify existing resume content.
6. You may use JD terminology only when the resume supports the underlying fact.
7. Do not generate LaTeX.
8. Do not generate markdown.
9. Return structured JSON only.
10. Preserve factual meaning.
11. If the JD requires skills that are not present in the master resume, place
    them in missingSkills.
12. Do not include missingSkills inside the tailored resume skills section.
13. If the target role is weakly supported by the master resume, add a tailoring
    note explaining that some JD areas are unsupported.
14. Do not over-position the candidate for a role that the master resume does
    not strongly support.

Fit warning rules:
- If many required or preferred JD skills are absent from the master resume,
  add a tailoring note.
- Use factual language such as "This role has missing skills not found in the
  master resume."
- Do not say the candidate is a strong fit unless the resume evidence supports it.

Contact info rules:
- Extract contact information only from the master resume.
- Do not invent missing phone, email, links, or location.
- If a contact field is not clearly present, return null.
- Preserve email, phone, LinkedIn, GitHub and portfolio URLs exactly where possible.

Professional summary rules:
- Concise and role-specific.
- Mention only technologies supported by the resume.
- Avoid generic buzzwords.
- Avoid unsupported claims.

Experience rules:
- Preserve company names, roles, and dates from the master resume.
- Select and rewrite relevant bullets.
- Do not fabricate impact metrics.
- Improve clarity and JD alignment.

Skills rules:
- Include matched JD skills only if supported by the resume.
- Do not include missing JD skills in tailored resume skills.
- Put missing JD skills into missingSkills.

Return JSON in this shape:
{
  "tailoredResume": {
    "contactInfo": {
      "fullName": string or null,
      "email": string or null,
      "phone": string or null,
      "location": string or null,
      "linkedinUrl": string or null,
      "githubUrl": string or null,
      "portfolioUrl": string or null
    },
    "professionalSummary": string,
    "skills": {
      "programmingLanguages": [string],
      "frameworks": [string],
      "databases": [string],
      "cloud": [string],
      "tools": [string],
      "other": [string]
    },
    "experience": [
      {
        "company": string,
        "role": string,
        "startDate": string,
        "endDate": string,
        "bullets": [string]
      }
    ],
    "projects": [
      {
        "name": string,
        "description": string,
        "technologies": [string],
        "bullets": [string]
      }
    ],
    "education": [string],
    "certifications": [string],
    "achievements": [string],
    "tailoringNotes": [string]
  },
  "matchedSkills": [
    {
      "skill": string,
      "matchType": "MATCHED",
      "evidence": string
    }
  ],
  "partiallyMatchedSkills": [
    {
      "skill": string,
      "matchType": "PARTIALLY_MATCHED",
      "evidence": string
    }
  ],
  "missingSkills": [
    {
      "skill": string,
      "matchType": "MISSING",
      "evidence": string
    }
  ],
  "tailoringNotes": [string]
}

Target job description structured intelligence:
Company: %s
Job title: %s
Required skills: %s
Preferred skills: %s
Responsibilities: %s
Required qualifications: %s
Preferred qualifications: %s
Keywords: %s

Master resume text:
%s
""";

    public static String buildPrompt(
            String company,
            String jobTitle,
            List<String> requiredSkills,
            List<String> preferredSkills,
            List<String> responsibilities,
            List<String> requiredQualifications,
            List<String> preferredQualifications,
            List<String> keywords,
            String masterResumeText) {

        return String.format(
                PROMPT_TEMPLATE,
                company != null ? company : "Unknown Company",
                jobTitle != null ? jobTitle : "Software Engineer",
                formatList(requiredSkills),
                formatList(preferredSkills),
                formatList(responsibilities),
                formatList(requiredQualifications),
                formatList(preferredQualifications),
                formatList(keywords),
                masterResumeText != null ? masterResumeText : ""
        );
    }

    private static String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "None specified";
        }
        return String.join(", ", items);
    }
}
