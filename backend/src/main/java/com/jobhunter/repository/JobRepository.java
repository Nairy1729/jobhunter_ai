package com.jobhunter.repository;

import com.jobhunter.model.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    Optional<Job> findByCanonicalUrlHash(String canonicalUrlHash);
    Optional<Job> findByContentHash(String contentHash);
    boolean existsByCanonicalUrlHash(String canonicalUrlHash);
    boolean existsByContentHash(String contentHash);
    Page<Job> findByActiveTrueOrderByPostingDateDesc(Pageable pageable);
    Page<Job> findByPipelineStatus(String pipelineStatus, Pageable pageable);
}
