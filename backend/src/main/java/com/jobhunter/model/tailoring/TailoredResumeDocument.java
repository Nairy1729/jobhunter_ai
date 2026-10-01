package com.jobhunter.model.tailoring;

import com.jobhunter.model.fact.SkillEvidenceType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Canonical Structured Resume Model.
 * Every field and bullet carries explicit references to verified CandidateFacts (sourceFactIds).
 * The document cannot be exported if any required evidence reference is missing or invalid.
 */
public class TailoredResumeDocument {

    private UUID id = UUID.randomUUID();
    private UUID candidateProfileId;
    private UUID jobId;
    private int versionNumber = 1;
    private ValidationStatus validationStatus = ValidationStatus.PENDING;
    private List<String> validationErrors = new ArrayList<>();

    private Header header = new Header();
    private Summary summary = new Summary();
    private List<SkillGroup> skillGroups = new ArrayList<>();
    private List<ExperienceItem> experiences = new ArrayList<>();
    private List<ProjectItem> projects = new ArrayList<>();
    private List<EducationItem> education = new ArrayList<>();
    private List<CertificationItem> certifications = new ArrayList<>();
    private List<AchievementItem> achievements = new ArrayList<>();

    public TailoredResumeDocument() {}

    // --- NESTED STRUCTURED COMPONENTS ---

    public static class Header {
        public String fullName;
        public String subTitle; // Headline or role title (e.g. Java Full Stack Developer | Software Engineer)
        public String email;
        public String phone;
        public String location;
        public String linkedinUrl;
        public String githubUrl;
        public List<UUID> sourceFactIds = new ArrayList<>();
    }

    public static class Summary {
        public String text;
        public List<UUID> sourceFactIds = new ArrayList<>();
        public String evidenceType = "DERIVED_FROM_SUPPORTED_FACTS"; // DIRECTLY_SUPPORTED, DERIVED_FROM_SUPPORTED_FACTS
        public ValidationStatus validationStatus = ValidationStatus.PENDING;
    }

    public static class SkillGroup {
        public String category; // Languages & Core, Frameworks & APIs, Databases & Infrastructure
        public List<SkillItem> skills = new ArrayList<>();

        public SkillGroup() {}
        public SkillGroup(String category) {
            this.category = category;
        }
    }

    public static class SkillItem {
        public String name;
        public SkillEvidenceType evidenceType = SkillEvidenceType.VERIFIED_SKILL;
        public UUID sourceFactId;

        public SkillItem() {}
        public SkillItem(String name, SkillEvidenceType evidenceType, UUID sourceFactId) {
            this.name = name;
            this.evidenceType = evidenceType;
            this.sourceFactId = sourceFactId;
        }
    }

    public static class ExperienceItem {
        public UUID sourceExperienceId;
        public String company;
        public String role;
        public String duration;
        public String location;
        public List<UUID> sourceFactIds = new ArrayList<>();
        public List<ExperienceBullet> bullets = new ArrayList<>();
    }

    public static class ExperienceBullet {
        public UUID id = UUID.randomUUID();
        public String text;
        public UUID sourceExperienceId;
        public List<UUID> sourceFactIds = new ArrayList<>();
        public String evidenceType = "DIRECTLY_SUPPORTED"; // DIRECTLY_SUPPORTED, DERIVED_FROM_SUPPORTED_FACTS
        public ValidationStatus validationStatus = ValidationStatus.PENDING;

        public ExperienceBullet() {}
        public ExperienceBullet(String text, UUID sourceExperienceId, List<UUID> sourceFactIds) {
            this.text = text;
            this.sourceExperienceId = sourceExperienceId;
            if (sourceFactIds != null) this.sourceFactIds.addAll(sourceFactIds);
        }
        public ExperienceBullet(String text, UUID sourceExperienceId, List<UUID> sourceFactIds, String evidenceType) {
            this.text = text;
            this.sourceExperienceId = sourceExperienceId;
            if (sourceFactIds != null) this.sourceFactIds.addAll(sourceFactIds);
            this.evidenceType = evidenceType;
        }
    }

    public static class ProjectItem {
        public UUID sourceProjectId;
        public String name;
        public String subTitle; // e.g. Enterprise Full-Stack Application, Full-Stack Personal Project
        public List<String> technologies = new ArrayList<>();
        public String projectUrl;
        public List<UUID> sourceFactIds = new ArrayList<>();
        public List<ProjectBullet> bullets = new ArrayList<>();
    }

    public static class ProjectBullet {
        public UUID id = UUID.randomUUID();
        public String text;
        public UUID sourceProjectId;
        public List<UUID> sourceFactIds = new ArrayList<>();
        public String evidenceType = "DIRECTLY_SUPPORTED"; // DIRECTLY_SUPPORTED, DERIVED_FROM_SUPPORTED_FACTS
        public ValidationStatus validationStatus = ValidationStatus.PENDING;

        public ProjectBullet() {}
        public ProjectBullet(String text, UUID sourceProjectId, List<UUID> sourceFactIds) {
            this.text = text;
            this.sourceProjectId = sourceProjectId;
            if (sourceFactIds != null) this.sourceFactIds.addAll(sourceFactIds);
        }
        public ProjectBullet(String text, UUID sourceProjectId, List<UUID> sourceFactIds, String evidenceType) {
            this.text = text;
            this.sourceProjectId = sourceProjectId;
            if (sourceFactIds != null) this.sourceFactIds.addAll(sourceFactIds);
            this.evidenceType = evidenceType;
        }
    }

    public static class EducationItem {
        public String degree;
        public String institution;
        public String dates;
        public String honors;
        public String grade; // e.g. 95.2% or CGPA
        public UUID sourceFactId;
        public ValidationStatus validationStatus = ValidationStatus.PENDING;
    }

    public static class CertificationItem {
        public String name;
        public String issuingOrganization;
        public String issueDate;
        public String credentialUrl;
        public UUID sourceFactId;
        public ValidationStatus validationStatus = ValidationStatus.PENDING;
    }

    public static class AchievementItem {
        public String title;
        public String description;
        public String rawText;
        public UUID sourceFactId;
        public ValidationStatus validationStatus = ValidationStatus.PENDING;

        public AchievementItem() {}
        public AchievementItem(String title, String description, String rawText) {
            this.title = title;
            this.description = description;
            this.rawText = rawText;
        }
    }

    // --- GETTERS & SETTERS ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCandidateProfileId() { return candidateProfileId; }
    public void setCandidateProfileId(UUID candidateProfileId) { this.candidateProfileId = candidateProfileId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public List<SkillGroup> getSkillGroups() { return skillGroups; }
    public void setSkillGroups(List<SkillGroup> skillGroups) { this.skillGroups = skillGroups; }

    public List<ExperienceItem> getExperiences() { return experiences; }
    public void setExperiences(List<ExperienceItem> experiences) { this.experiences = experiences; }

    public List<ProjectItem> getProjects() { return projects; }
    public void setProjects(List<ProjectItem> projects) { this.projects = projects; }

    public List<EducationItem> getEducation() { return education; }
    public void setEducation(List<EducationItem> education) { this.education = education; }

    public List<CertificationItem> getCertifications() { return certifications; }
    public void setCertifications(List<CertificationItem> certifications) { this.certifications = certifications; }

    public List<AchievementItem> getAchievements() { return achievements; }
    public void setAchievements(List<AchievementItem> achievements) { this.achievements = achievements; }
}
