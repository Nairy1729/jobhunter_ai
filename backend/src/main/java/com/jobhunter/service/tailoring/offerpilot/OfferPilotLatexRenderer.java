package com.jobhunter.service.tailoring.offerpilot;

import com.jobhunter.dto.tailoring.offerpilot.TailoredResumePayload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic LaTeX Renderer & Two-Stage Character Escaper.
 * Implements Section 8 of the OfferPilot Architecture Blueprint.
 */
@Service
public class OfferPilotLatexRenderer {

    /**
     * Escapes text using OfferPilot Two-Stage Escaping:
     * Stage 1: Typography Normalization
     * Stage 2: TeX Control Character Escaping
     */
    public static String escapeLatex(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // --- STAGE 1: Typography Normalization ---
        String s = text
                .replace("\u2013", "--")     // En-dash
                .replace("\u2014", "---")    // Em-dash
                .replace("’", "'")           // Smart right single quote / apostrophe
                .replace("‘", "'")           // Smart left single quote
                .replace("“", "\"")          // Smart left double quote
                .replace("”", "\"");         // Smart right double quote

        // --- STAGE 2: TeX Control Character Escaping ---
        // Backslash must be escaped first to prevent escaping subsequent backslashes
        s = s.replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");

        return s;
    }

    /**
     * Renders a TailoredResumeContent object into clean, deterministic ATS-optimized LaTeX source code.
     */
    public String renderLatex(TailoredResumePayload.TailoredResumeContent content) {
        if (content == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Preamble
        sb.append("\\documentclass[10pt,a4paper]{article}\n");
        sb.append("\\usepackage[margin=0.5in]{geometry}\n");
        sb.append("\\usepackage{enumitem}\n");
        sb.append("\\usepackage[hidelinks]{hyperref}\n");
        sb.append("\\usepackage{titlesec}\n");
        sb.append("\\usepackage{parskip}\n");
        sb.append("\\pagenumbering{gobble}\n\n");
        sb.append("\\titleformat{\\section}{\\vspace{-4pt}\\large\\bfseries}{}{0em}{}[\\titlerule\\vspace{-2pt}]\n");
        sb.append("\\setlist[itemize]{leftmargin=*, noitemsep, topsep=2pt}\n\n");
        sb.append("\\begin{document}\n\n");

        // Contact Header
        TailoredResumePayload.ContactInfo contact = content.getContactInfo();
        String fullName = contact != null && contact.getFullName() != null && !contact.getFullName().isBlank()
                ? escapeLatex(contact.getFullName())
                : "Candidate";

        sb.append("% Contact Header\n");
        sb.append("\\begin{center}\n");
        sb.append("{\\LARGE \\textbf{").append(fullName).append("}}\\\\\n");

        List<String> contactParts = new ArrayList<>();
        if (contact != null) {
            if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
                contactParts.add(escapeLatex(contact.getEmail()));
            }
            if (contact.getPhone() != null && !contact.getPhone().isBlank()) {
                contactParts.add(escapeLatex(contact.getPhone()));
            }
            if (contact.getLocation() != null && !contact.getLocation().isBlank()) {
                contactParts.add(escapeLatex(contact.getLocation()));
            }
            if (contact.getLinkedinUrl() != null && !contact.getLinkedinUrl().isBlank()) {
                contactParts.add("\\href{" + contact.getLinkedinUrl() + "}{LinkedIn}");
            }
            if (contact.getGithubUrl() != null && !contact.getGithubUrl().isBlank()) {
                contactParts.add("\\href{" + contact.getGithubUrl() + "}{GitHub}");
            }
            if (contact.getPortfolioUrl() != null && !contact.getPortfolioUrl().isBlank()) {
                contactParts.add("\\href{" + contact.getPortfolioUrl() + "}{Portfolio}");
            }
        }
        sb.append(String.join(" | ", contactParts)).append("\n");
        sb.append("\\end{center}\n\n");

        // Professional Summary
        if (content.getProfessionalSummary() != null && !content.getProfessionalSummary().isBlank()) {
            sb.append("% Professional Summary\n");
            sb.append("\\section*{Professional Summary}\n");
            sb.append(escapeLatex(content.getProfessionalSummary())).append("\n\n");
        }

        // Technical Skills
        TailoredResumePayload.SkillsContainer skills = content.getSkills();
        if (skills != null && hasAnySkills(skills)) {
            sb.append("% Technical Skills\n");
            sb.append("\\section*{Technical Skills}\n");

            appendSkillLine(sb, "Programming Languages", skills.getProgrammingLanguages());
            appendSkillLine(sb, "Frameworks", skills.getFrameworks());
            appendSkillLine(sb, "Databases", skills.getDatabases());
            appendSkillLine(sb, "Cloud", skills.getCloud());
            appendSkillLine(sb, "Tools", skills.getTools());
            appendSkillLine(sb, "Other", skills.getOther());
            sb.append("\n");
        }

        // Work Experience
        List<TailoredResumePayload.ExperienceEntry> experience = content.getExperience();
        if (experience != null && !experience.isEmpty()) {
            sb.append("% Work Experience\n");
            sb.append("\\section*{Experience}\n");

            for (TailoredResumePayload.ExperienceEntry exp : experience) {
                String role = exp.getRole() != null ? escapeLatex(exp.getRole()) : "Software Engineer";
                String company = exp.getCompany() != null ? escapeLatex(exp.getCompany()) : "Company";
                String startDate = exp.getStartDate() != null ? escapeLatex(exp.getStartDate()) : "";
                String endDate = exp.getEndDate() != null ? escapeLatex(exp.getEndDate()) : "Present";

                sb.append("\\textbf{").append(role).append("} -- ").append(company)
                        .append("\\hfill ").append(startDate).append(" -- ").append(endDate).append("\n");

                List<String> bullets = exp.getBullets();
                if (bullets != null && !bullets.isEmpty()) {
                    sb.append("\\begin{itemize}\n");
                    for (String bullet : bullets) {
                        if (bullet != null && !bullet.isBlank()) {
                            sb.append("\\item ").append(escapeLatex(bullet)).append("\n");
                        }
                    }
                    sb.append("\\end{itemize}\n");
                }
                sb.append("\\vspace{4pt}\n");
            }
            sb.append("\n");
        }

        // Projects
        List<TailoredResumePayload.ProjectEntry> projects = content.getProjects();
        if (projects != null && !projects.isEmpty()) {
            sb.append("% Projects\n");
            sb.append("\\section*{Projects}\n");

            for (TailoredResumePayload.ProjectEntry proj : projects) {
                String name = proj.getName() != null ? escapeLatex(proj.getName()) : "Project";
                String tech = proj.getTechnologies() != null && !proj.getTechnologies().isEmpty()
                        ? escapeLatex(String.join(", ", proj.getTechnologies()))
                        : "";

                sb.append("\\textbf{").append(name).append("}");
                if (!tech.isBlank()) {
                    sb.append(" -- ").append(tech);
                }
                sb.append("\\\\\n");

                if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                    sb.append("\\textit{").append(escapeLatex(proj.getDescription())).append("}\n");
                }

                List<String> bullets = proj.getBullets();
                if (bullets != null && !bullets.isEmpty()) {
                    sb.append("\\begin{itemize}\n");
                    for (String bullet : bullets) {
                        if (bullet != null && !bullet.isBlank()) {
                            sb.append("\\item ").append(escapeLatex(bullet)).append("\n");
                        }
                    }
                    sb.append("\\end{itemize}\n");
                }
                sb.append("\\vspace{4pt}\n");
            }
            sb.append("\n");
        }

        // Education
        List<String> education = content.getEducation();
        if (education != null && !education.isEmpty()) {
            sb.append("% Education\n");
            sb.append("\\section*{Education}\n");
            sb.append("\\begin{itemize}\n");
            for (String edu : education) {
                if (edu != null && !edu.isBlank()) {
                    sb.append("\\item ").append(escapeLatex(edu)).append("\n");
                }
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Certifications
        List<String> certs = content.getCertifications();
        if (certs != null && !certs.isEmpty()) {
            sb.append("% Certifications\n");
            sb.append("\\section*{Certifications}\n");
            sb.append("\\begin{itemize}\n");
            for (String cert : certs) {
                if (cert != null && !cert.isBlank()) {
                    sb.append("\\item ").append(escapeLatex(cert)).append("\n");
                }
            }
            sb.append("\\end{itemize}\n\n");
        }

        // Achievements
        List<String> achievements = content.getAchievements();
        if (achievements != null && !achievements.isEmpty()) {
            sb.append("% Achievements\n");
            sb.append("\\section*{Achievements}\n");
            sb.append("\\begin{itemize}\n");
            for (String ach : achievements) {
                if (ach != null && !ach.isBlank()) {
                    sb.append("\\item ").append(escapeLatex(ach)).append("\n");
                }
            }
            sb.append("\\end{itemize}\n\n");
        }

        sb.append("\\end{document}\n");
        return sb.toString();
    }

    private boolean hasAnySkills(TailoredResumePayload.SkillsContainer skills) {
        return (skills.getProgrammingLanguages() != null && !skills.getProgrammingLanguages().isEmpty())
                || (skills.getFrameworks() != null && !skills.getFrameworks().isEmpty())
                || (skills.getDatabases() != null && !skills.getDatabases().isEmpty())
                || (skills.getCloud() != null && !skills.getCloud().isEmpty())
                || (skills.getTools() != null && !skills.getTools().isEmpty())
                || (skills.getOther() != null && !skills.getOther().isEmpty());
    }

    private void appendSkillLine(StringBuilder sb, String label, List<String> items) {
        if (items != null && !items.isEmpty()) {
            String joined = items.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(OfferPilotLatexRenderer::escapeLatex)
                    .collect(Collectors.joining(", "));
            if (!joined.isBlank()) {
                sb.append("\\textbf{").append(label).append(":} ").append(joined).append("\\\\\n");
            }
        }
    }
}
