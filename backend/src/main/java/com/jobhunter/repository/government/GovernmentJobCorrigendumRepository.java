package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.GovernmentJobCorrigendum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GovernmentJobCorrigendumRepository extends JpaRepository<GovernmentJobCorrigendum, UUID> {
    List<GovernmentJobCorrigendum> findByJobIdOrderByIssueDateDesc(UUID jobId);
}
