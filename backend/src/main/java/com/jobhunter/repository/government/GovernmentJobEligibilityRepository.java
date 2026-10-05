package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.GovernmentJobEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentJobEligibilityRepository extends JpaRepository<GovernmentJobEligibility, UUID> {
    Optional<GovernmentJobEligibility> findByJobId(UUID jobId);
}
