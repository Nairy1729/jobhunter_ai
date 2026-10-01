package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, UUID> {
    List<CandidateExperience> findByCandidateProfileId(UUID candidateProfileId);
    void deleteByCandidateProfileId(UUID candidateProfileId);
}
