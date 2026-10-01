package com.jobhunter.dto.tailoring.offerpilot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TailoredResumePayload {

    private TailoredResumeContent tailoredResume = new TailoredResumeContent();
    private List<SkillClassification> matchedSkills = new ArrayList<>();
    private List<SkillClassification> partiallyMatchedSkills = new ArrayList<>();
    private List<SkillClassification> missingSkills = new ArrayList<>();
    private List<String> tailoringNotes = new ArrayList<>();

    public TailoredResumePayload() {}

    public TailoredResumeContent getTailoredResume() { return tailoredResume; }
    public void setTailoredResume(TailoredResumeContent tailoredResume) { this.tailoredResume = tailoredResume; }

    public List<SkillClassification> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<SkillClassification> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<SkillClassification> getPartiallyMatchedSkills() { return partiallyMatchedSkills; }
    public void setPartiallyMatchedSkills(List<SkillClassification> partiallyMatchedSkills) { this.partiallyMatchedSkills = partiallyMatchedSkills; }

    public List<SkillClassification> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<SkillClassification> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getTailoringNotes() { return tailoringNotes; }
    public void setTailoringNotes(List<String> tailoringNotes) { this.tailoringNotes = tailoringNotes; }

    // --- NESTED CLASSES ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TailoredResumeContent {
        private ContactInfo contactInfo = new ContactInfo();
        private String professionalSummary = "";
        private SkillsContainer skills = new SkillsContainer();
        private List<ExperienceEntry> experience = new ArrayList<>();
        private List<ProjectEntry> projects = new ArrayList<>();
        private List<String> education = new ArrayList<>();
        private List<String> certifications = new ArrayList<>();
        private List<String> achievements = new ArrayList<>();
        private List<String> tailoringNotes = new ArrayList<>();

        public TailoredResumeContent() {}

        public ContactInfo getContactInfo() { return contactInfo; }
        public void setContactInfo(ContactInfo contactInfo) { this.contactInfo = contactInfo; }

        public String getProfessionalSummary() { return professionalSummary; }
        public void setProfessionalSummary(String professionalSummary) { this.professionalSummary = professionalSummary; }

        public SkillsContainer getSkills() { return skills; }
        public void setSkills(SkillsContainer skills) { this.skills = skills; }

        public List<ExperienceEntry> getExperience() { return experience; }
        public void setExperience(List<ExperienceEntry> experience) { this.experience = experience; }

        public List<ProjectEntry> getProjects() { return projects; }
        public void setProjects(List<ProjectEntry> projects) { this.projects = projects; }

        public List<String> getEducation() { return education; }
        public void setEducation(List<String> education) { this.education = education; }

        public List<String> getCertifications() { return certifications; }
        public void setCertifications(List<String> certifications) { this.certifications = certifications; }

        public List<String> getAchievements() { return achievements; }
        public void setAchievements(List<String> achievements) { this.achievements = achievements; }

        public List<String> getTailoringNotes() { return tailoringNotes; }
        public void setTailoringNotes(List<String> tailoringNotes) { this.tailoringNotes = tailoringNotes; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactInfo {
        private String fullName;
        private String email;
        private String phone;
        private String location;
        private String linkedinUrl;
        private String githubUrl;
        private String portfolioUrl;

        public ContactInfo() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getLinkedinUrl() { return linkedinUrl; }
        public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

        public String getGithubUrl() { return githubUrl; }
        public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

        public String getPortfolioUrl() { return portfolioUrl; }
        public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillsContainer {
        private List<String> programmingLanguages = new ArrayList<>();
        private List<String> frameworks = new ArrayList<>();
        private List<String> databases = new ArrayList<>();
        private List<String> cloud = new ArrayList<>();
        private List<String> tools = new ArrayList<>();
        private List<String> other = new ArrayList<>();

        public SkillsContainer() {}

        public List<String> getProgrammingLanguages() { return programmingLanguages; }
        public void setProgrammingLanguages(List<String> programmingLanguages) { this.programmingLanguages = programmingLanguages; }

        public List<String> getFrameworks() { return frameworks; }
        public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }

        public List<String> getDatabases() { return databases; }
        public void setDatabases(List<String> databases) { this.databases = databases; }

        public List<String> getCloud() { return cloud; }
        public void setCloud(List<String> cloud) { this.cloud = cloud; }

        public List<String> getTools() { return tools; }
        public void setTools(List<String> tools) { this.tools = tools; }

        public List<String> getOther() { return other; }
        public void setOther(List<String> other) { this.other = other; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceEntry {
        private String company;
        private String role;
        private String startDate;
        private String endDate;
        private List<String> bullets = new ArrayList<>();

        public ExperienceEntry() {}

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectEntry {
        private String name;
        private String description;
        private List<String> technologies = new ArrayList<>();
        private List<String> bullets = new ArrayList<>();

        public ProjectEntry() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getTechnologies() { return technologies; }
        public void setTechnologies(List<String> technologies) { this.technologies = technologies; }

        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets; }
    }
}
