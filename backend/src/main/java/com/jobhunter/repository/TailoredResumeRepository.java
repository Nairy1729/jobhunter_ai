package com.jobhunter.repository;

import com.jobhunter.model.entity.TailoredResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TailoredResumeRepository extends JpaRepository<TailoredResume, UUID> {

    List<TailoredResume> findByCandidateProfileIdAndJobId(UUID candidateProfileId, UUID jobId);

    Optional<TailoredResume> findTopByCandidateProfileIdAndJobIdOrderByVersionNumberDesc(UUID candidateProfileId, UUID jobId);

    List<TailoredResume> findByCandidateProfileIdOrderByCreatedAtDesc(UUID candidateProfileId);

    @Query("SELECT tr FROM TailoredResume tr WHERE tr.id = :id AND tr.candidateProfile.user.id = :userId")
    Optional<TailoredResume> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT tr FROM TailoredResume tr WHERE tr.job.id = :jobId AND tr.candidateProfile.user.id = :userId ORDER BY tr.versionNumber DESC")
    List<TailoredResume> findByJobIdAndUserIdOrderByVersionDesc(@Param("jobId") UUID jobId, @Param("userId") UUID userId);
}
