package com.jobhunter.repository;

import com.jobhunter.model.entity.JobSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobSourceRepository extends JpaRepository<JobSource, UUID> {
    Optional<JobSource> findByName(String name);
}
