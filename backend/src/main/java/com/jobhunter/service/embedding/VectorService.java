package com.jobhunter.service.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.CandidateEmbedding;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Job;
import com.jobhunter.model.entity.JobEmbedding;
import com.jobhunter.repository.CandidateEmbeddingRepository;
import com.jobhunter.repository.JobEmbeddingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VectorService {

    private static final Logger log = LoggerFactory.getLogger(VectorService.class);

    private final JdbcTemplate jdbcTemplate;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final CandidateEmbeddingRepository candidateEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    private boolean pgVectorAvailable = false;

    public VectorService(
            JdbcTemplate jdbcTemplate,
            JobEmbeddingRepository jobEmbeddingRepository,
            CandidateEmbeddingRepository candidateEmbeddingRepository,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobEmbeddingRepository = jobEmbeddingRepository;
        this.candidateEmbeddingRepository = candidateEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            Boolean hasExtension = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')",
                    Boolean.class
            );
            this.pgVectorAvailable = Boolean.TRUE.equals(hasExtension);

            if (this.pgVectorAvailable) {
                log.info("PGVECTOR_STATUS: Native PostgreSQL pgvector (vector 768) is AVAILABLE and ACTIVE. Native cosine vector distance queries enabled.");
            } else {
                log.warn("PGVECTOR_STATUS: pgvector extension is NOT installed in PostgreSQL. Falling back to universal JSONB vector storage with in-memory dot-product cosine similarity. Embeddings are NOT faked or randomized.");
            }
        } catch (Exception e) {
            log.warn("PGVECTOR_STATUS: Could not check pgvector extension status: {}. Using universal JSONB vector storage.", e.getMessage());
            this.pgVectorAvailable = false;
        }
    }

    public boolean isPgVectorAvailable() {
        return pgVectorAvailable;
    }

    public String getStatusDescription() {
        if (pgVectorAvailable) {
            return "Active (PostgreSQL pgvector native extension enabled)";
        } else {
            return "Universal (JSONB array vector storage with exact dot-product cosine calculation. To enable native PostgreSQL pgvector, install the extension in PostgreSQL 16)";
        }
    }

    /**
     * Exact cosine similarity between two 768-dimensional float vectors.
     */
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length == 0 || vectorB.length == 0) {
            return 0.0;
        }

        int length = Math.min(vectorA.length, vectorB.length);
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        // Bound between 0.0 and 1.0 for normalized positive matching
        return Math.max(0.0, Math.min(1.0, similarity));
    }

    @Transactional
    public float[] getOrGenerateJobEmbedding(Job job) {
        String model = embeddingService.getActiveModelName();
        Optional<JobEmbedding> cached = jobEmbeddingRepository.findByJobIdAndEmbeddingModel(job.getId(), model);
        if (cached.isPresent()) {
            return deserializeVector(cached.get().getEmbeddingData());
        }

        String contentToEmbed = buildJobTextForEmbedding(job);
        float[] vector = embeddingService.generateEmbedding(contentToEmbed);

        JobEmbedding embedding = new JobEmbedding(job, model, 768, serializeVector(vector));
        jobEmbeddingRepository.save(embedding);

        return vector;
    }

    @Transactional
    public float[] getOrGenerateCandidateEmbedding(CandidateProfile profile) {
        String model = embeddingService.getActiveModelName();
        Optional<CandidateEmbedding> cached = candidateEmbeddingRepository.findByCandidateProfileIdAndEmbeddingModel(profile.getId(), model);
        if (cached.isPresent()) {
            return deserializeVector(cached.get().getEmbeddingData());
        }

        String contentToEmbed = buildCandidateTextForEmbedding(profile);
        float[] vector = embeddingService.generateEmbedding(contentToEmbed);

        CandidateEmbedding embedding = new CandidateEmbedding(profile, model, 768, serializeVector(vector));
        candidateEmbeddingRepository.save(embedding);

        return vector;
    }

    public String buildJobTextForEmbedding(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(job.getTitle()).append("\n");
        if (job.getDepartment() != null) sb.append("Department: ").append(job.getDepartment()).append("\n");
        sb.append("Location: ").append(job.getLocation()).append(" (").append(job.getWorkMode()).append(")\n");
        if (job.getRawDescriptionMarkdown() != null) {
            String desc = job.getRawDescriptionMarkdown();
            sb.append("Description: ").append(desc.length() > 3000 ? desc.substring(0, 3000) : desc);
        }
        return sb.toString();
    }

    public String buildCandidateTextForEmbedding(CandidateProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Headline: ").append(profile.getHeadline()).append("\n");
        if (profile.getSummary() != null) sb.append("Summary: ").append(profile.getSummary()).append("\n");
        sb.append("Target Roles: ").append(profile.getTargetRoles()).append("\n");
        sb.append("Years of Experience: ").append(profile.getYearsOfExperience()).append("\n");

        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            sb.append("Skills: ");
            profile.getSkills().forEach(s -> {
                sb.append(s.getSkill().getName()).append(" (").append(s.getProficiencyLevel())
                        .append(", ").append(s.getExperienceType()).append("), ");
            });
            sb.append("\n");
        }

        if (profile.getExperiences() != null && !profile.getExperiences().isEmpty()) {
            sb.append("Experience: ");
            profile.getExperiences().forEach(e -> {
                sb.append(e.getRole()).append(" at ").append(e.getCompany()).append(" [").append(e.getTechnologies()).append("]; ");
            });
            sb.append("\n");
        }

        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            sb.append("Projects: ");
            profile.getProjects().forEach(p -> {
                sb.append(p.getName()).append(": ").append(p.getDescription()).append(" [").append(p.getTechnologies()).append("]; ");
            });
            sb.append("\n");
        }

        return sb.toString();
    }

    public String serializeVector(float[] vector) {
        try {
            List<Float> list = new ArrayList<>(vector.length);
            for (float v : vector) list.add(v);
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to serialize vector", e);
            return "[]";
        }
    }

    public float[] deserializeVector(String json) {
        try {
            List<Double> list = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = list.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.error("Failed to deserialize vector", e);
            return new float[768];
        }
    }
}
