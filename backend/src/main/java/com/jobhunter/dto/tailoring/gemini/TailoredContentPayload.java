package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured Tailored Content returned by Gemini in Stage 5.
 * Strictly adheres to: Maximum linguistic freedom + zero factual fabrication.
 */
public class TailoredContentPayload {

    public String summary;
    public BaseResumeModel.SkillsCategorized skills = new BaseResumeModel.SkillsCategorized();
    public List<TailoredExperience> experience = new ArrayList<>();
    public List<TailoredProject> projects = new ArrayList<>();
    public List<BaseResumeModel.EducationModel> education = new ArrayList<>();
    public List<BaseResumeModel.AchievementModel> achievements = new ArrayList<>();
    public List<String> missingSkills = new ArrayList<>();
    public List<String> tailoringNotes = new ArrayList<>();

    public static class TailoredExperience {
        public String id;
        public String company;
        public String title;
        public String dates;
        public String location;
        public List<TailoredBullet> bullets = new ArrayList<>();
    }

    public static class TailoredProject {
        public String id;
        public String name;
        public String subTitle;
        public String projectUrl;
        public List<String> technologies = new ArrayList<>();
        public List<TailoredBullet> bullets = new ArrayList<>();
    }

    public static class TailoredBullet {
        public String text;
        public List<String> sourceFacts = new ArrayList<>();
        public List<String> sourceIds = new ArrayList<>();
        public String action = "rewrite"; // "rewrite", "keep", "synthesis", "condense", "reorder"
        public String validationStatus = "SUPPORTED"; // "SUPPORTED", "DERIVED", "UNSUPPORTED"

        public TailoredBullet() {}

        public TailoredBullet(String text, List<String> sourceFacts, String action) {
            this.text = text;
            this.sourceFacts = sourceFacts != null ? sourceFacts : new ArrayList<>();
            this.sourceIds = new ArrayList<>(this.sourceFacts);
            this.action = action;
        }

        public List<String> getEffectiveSourceIds() {
            if (sourceIds != null && !sourceIds.isEmpty()) return sourceIds;
            if (sourceFacts != null && !sourceFacts.isEmpty()) return sourceFacts;
            return new ArrayList<>();
        }
    }
}
