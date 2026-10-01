package com.jobhunter.repository;

import com.jobhunter.model.entity.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {
    Optional<JobEmbedding> findByJobIdAndEmbeddingModel(UUID jobId, String embeddingModel);
    void deleteByJobId(UUID jobId);
}
