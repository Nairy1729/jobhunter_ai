package com.jobhunter.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_embeddings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_profile_id", "embedding_model"}))
public class CandidateEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(nullable = false)
    private int dimensions = 768;

    @Column(name = "embedding_data", columnDefinition = "jsonb", nullable = false)
    private String embeddingData; // JSON array of 768 float numbers

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CandidateEmbedding() {}

    public CandidateEmbedding(CandidateProfile candidateProfile, String embeddingModel, int dimensions, String embeddingData) {
        this.candidateProfile = candidateProfile;
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.embeddingData = embeddingData;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public int getDimensions() { return dimensions; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }

    public String getEmbeddingData() { return embeddingData; }
    public void setEmbeddingData(String embeddingData) { this.embeddingData = embeddingData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
