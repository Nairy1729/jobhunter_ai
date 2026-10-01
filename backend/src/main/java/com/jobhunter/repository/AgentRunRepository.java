package com.jobhunter.repository;

import com.jobhunter.model.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
    List<AgentRun> findTop20ByOrderByCreatedAtDesc();
    List<AgentRun> findByAgentNameOrderByCreatedAtDesc(String agentName);
}
