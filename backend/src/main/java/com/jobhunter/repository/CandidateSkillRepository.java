package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {
    List<CandidateSkill> findByCandidateProfileId(UUID candidateProfileId);
    Optional<CandidateSkill> findByCandidateProfileIdAndSkillId(UUID candidateProfileId, UUID skillId);
    void deleteByCandidateProfileId(UUID candidateProfileId);
}
