package com.jobhunter.repository;

import com.jobhunter.model.entity.JobMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    Optional<JobMatch> findByJobIdAndCandidateProfileId(UUID jobId, UUID candidateProfileId);

    java.util.List<JobMatch> findByCandidateProfileId(UUID candidateProfileId);

    java.util.List<JobMatch> findByCandidateProfileIdAndJobIdIn(UUID candidateProfileId, java.util.Collection<UUID> jobIds);

    @Query("SELECT m FROM JobMatch m " +
           "JOIN FETCH m.job j " +
           "JOIN FETCH j.company c " +
           "WHERE m.candidateProfile.id = :profileId " +
           "AND m.queueTier = :queueTier " +
           "AND j.active = true " +
           "ORDER BY m.priorityScore DESC, j.postingDate DESC")
    Page<JobMatch> findByProfileIdAndQueueTier(
            @Param("profileId") UUID profileId,
            @Param("queueTier") String queueTier,
            Pageable pageable);
}
