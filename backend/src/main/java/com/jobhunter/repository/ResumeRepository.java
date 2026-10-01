package com.jobhunter.repository;

import com.jobhunter.model.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByCandidateProfileId(UUID candidateProfileId);
    Optional<Resume> findFirstByCandidateProfileIdAndMasterTrueOrderByCreatedAtDesc(UUID candidateProfileId);
}
