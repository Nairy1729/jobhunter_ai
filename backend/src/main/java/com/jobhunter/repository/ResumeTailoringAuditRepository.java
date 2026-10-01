package com.jobhunter.repository;

import com.jobhunter.model.entity.ResumeTailoringAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeTailoringAuditRepository extends JpaRepository<ResumeTailoringAudit, UUID> {

    List<ResumeTailoringAudit> findByCandidateProfileIdOrderByGeneratedAtDesc(UUID candidateProfileId);

    List<ResumeTailoringAudit> findByJobIdOrderByGeneratedAtDesc(UUID jobId);
}
