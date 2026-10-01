package com.jobhunter.repository;

import com.jobhunter.model.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByCandidateProfileIdAndJobId(UUID candidateProfileId, UUID jobId);

    List<Application> findByCandidateProfileIdAndAppliedTrue(UUID candidateProfileId);

    boolean existsByCandidateProfileIdAndJobIdAndAppliedTrue(UUID candidateProfileId, UUID jobId);
}
