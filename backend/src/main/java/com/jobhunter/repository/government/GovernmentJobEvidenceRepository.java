package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.GovernmentJobEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GovernmentJobEvidenceRepository extends JpaRepository<GovernmentJobEvidence, UUID> {
    List<GovernmentJobEvidence> findByJobId(UUID jobId);
}
