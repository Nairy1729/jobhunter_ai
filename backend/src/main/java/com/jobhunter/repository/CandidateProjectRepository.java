package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateProjectRepository extends JpaRepository<CandidateProject, UUID> {
    List<CandidateProject> findByCandidateProfileId(UUID candidateProfileId);
    void deleteByCandidateProfileId(UUID candidateProfileId);
}
