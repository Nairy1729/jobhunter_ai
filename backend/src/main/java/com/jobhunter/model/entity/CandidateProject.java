package com.jobhunter.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_projects")
public class CandidateProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String technologies = "[]";

    @Column(columnDefinition = "TEXT")
    private String architecture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String responsibilities = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "measurable_outcomes", columnDefinition = "jsonb", nullable = false)
    private String measurableOutcomes = "[]";

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CandidateProject() {}

    public CandidateProject(CandidateProfile candidateProfile, String name, String description,
                            String technologies, String architecture, String responsibilities,
                            String measurableOutcomes, String evidenceText, String projectUrl) {
        this.candidateProfile = candidateProfile;
        this.name = name;
        this.description = description;
        this.technologies = technologies != null ? technologies : "[]";
        this.architecture = architecture;
        this.responsibilities = responsibilities != null ? responsibilities : "[]";
        this.measurableOutcomes = measurableOutcomes != null ? measurableOutcomes : "[]";
        this.evidenceText = evidenceText;
        this.projectUrl = projectUrl;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }

    public String getMeasurableOutcomes() { return measurableOutcomes; }
    public void setMeasurableOutcomes(String measurableOutcomes) { this.measurableOutcomes = measurableOutcomes; }

    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
