package com.jobhunter.repository;

import com.jobhunter.model.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    
    Optional<CandidateProfile> findByUserId(UUID userId);

    @Query("SELECT p FROM CandidateProfile p " +
           "LEFT JOIN FETCH p.skills s " +
           "LEFT JOIN FETCH s.skill " +
           "WHERE p.user.id = :userId")
    Optional<CandidateProfile> findByUserIdWithSkills(@Param("userId") UUID userId);
}
