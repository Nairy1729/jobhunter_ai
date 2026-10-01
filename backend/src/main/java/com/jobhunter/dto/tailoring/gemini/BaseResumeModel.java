package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured Base Resume Model representing the complete factual ground truth of the candidate.
 * Stage 1 of the Gemini AI Resume Tailoring Pipeline.
 */
public class BaseResumeModel {

    public CandidateIdentity candidate = new CandidateIdentity();
    public String summary;
    public SkillsCategorized skills = new SkillsCategorized();
    public List<ExperienceModel> experience = new ArrayList<>();
    public List<ProjectModel> projects = new ArrayList<>();
    public List<EducationModel> education = new ArrayList<>();
    public List<String> certifications = new ArrayList<>();
    public List<AchievementModel> achievements = new ArrayList<>();

    public static class CandidateIdentity {
        public String name;
        public String headline;
        public String email;
        public String phone;
        public String location;
        public String linkedinUrl;
        public String githubUrl;
        public String portfolioUrl;
    }

    public static class SkillsCategorized {
        public List<String> languages = new ArrayList<>();
        public List<String> backend = new ArrayList<>();
        public List<String> frontend = new ArrayList<>();
        public List<String> databases = new ArrayList<>();
        public List<String> testing = new ArrayList<>();
        public List<String> devops = new ArrayList<>();
        public List<String> concepts = new ArrayList<>();

        public List<String> getAllSkillsFlat() {
            List<String> all = new ArrayList<>();
            all.addAll(languages);
            all.addAll(backend);
            all.addAll(frontend);
            all.addAll(databases);
            all.addAll(testing);
            all.addAll(devops);
            all.addAll(concepts);
            return all;
        }
    }

    public static class ExperienceModel {
        public String id;
        public String company;
        public String title;
        public String dates;
        public String location;
        public List<String> facts = new ArrayList<>();
        public List<String> technologies = new ArrayList<>();
        public List<String> responsibilities = new ArrayList<>();
        public List<String> achievements = new ArrayList<>();
    }

    public static class ProjectModel {
        public String id;
        public String name;
        public String subTitle;
        public String projectUrl;
        public List<String> technologies = new ArrayList<>();
        public List<String> responsibilities = new ArrayList<>();
        public List<String> measurableOutcomes = new ArrayList<>();
    }

    public static class EducationModel {
        public String institution;
        public String degree;
        public String dates;
        public String grade;
    }

    public static class AchievementModel {
        public String title;
        public String description;
        public String rawText;

        public AchievementModel() {}

        public AchievementModel(String title, String description, String rawText) {
            this.title = title;
            this.description = description;
            this.rawText = rawText;
        }
    }
}
