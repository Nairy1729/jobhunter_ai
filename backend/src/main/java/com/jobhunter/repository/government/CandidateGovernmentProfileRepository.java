package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.CandidateGovernmentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateGovernmentProfileRepository extends JpaRepository<CandidateGovernmentProfile, UUID> {
    Optional<CandidateGovernmentProfile> findByUserId(UUID userId);
}
