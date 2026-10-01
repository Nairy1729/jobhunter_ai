package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateFact;
import com.jobhunter.model.fact.FactCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateFactRepository extends JpaRepository<CandidateFact, UUID> {

    List<CandidateFact> findByCandidateProfileId(UUID candidateProfileId);

    List<CandidateFact> findByCandidateProfileIdAndCategory(UUID candidateProfileId, FactCategory category);

    void deleteByCandidateProfileId(UUID candidateProfileId);
}
