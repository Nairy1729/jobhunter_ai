package com.jobhunter.dto.tailoring.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit Tailoring Plan detailing action for every master-resume element.
 * Stage 4 of the Gemini AI Resume Tailoring Pipeline.
 */
public class GeminiTailoringPlan {

    public String summaryAction = "REWRITE";
    public List<String> skillsToLead = new ArrayList<>();
    public List<String> skillsToDeemphasize = new ArrayList<>();
    public List<BulletActionPlan> bulletPlans = new ArrayList<>();
    public List<ProjectActionPlan> projectPlans = new ArrayList<>();
    public String educationAction = "KEEP_EXACT";
    public String achievementsAction = "KEEP_EXACT";

    public static class BulletActionPlan {
        public String experienceId;
        public String originalBullet;
        public String action; // "REWRITE", "KEEP", "CONDENSE"
        public String proposedBullet;
        public List<String> sourceFacts = new ArrayList<>();

        public BulletActionPlan() {}

        public BulletActionPlan(String experienceId, String originalBullet, String action, String proposedBullet) {
            this.experienceId = experienceId;
            this.originalBullet = originalBullet;
            this.action = action;
            this.proposedBullet = proposedBullet;
        }
    }

    public static class ProjectActionPlan {
        public String projectId;
        public String name;
        public String action; // "EMPHASIZE_BACKEND", "EMPHASIZE_FULLSTACK", "CONDENSE", "KEEP"
        public String tailoredSubTitle;

        public ProjectActionPlan() {}

        public ProjectActionPlan(String projectId, String name, String action) {
            this.projectId = projectId;
            this.name = name;
            this.action = action;
        }
    }
}
