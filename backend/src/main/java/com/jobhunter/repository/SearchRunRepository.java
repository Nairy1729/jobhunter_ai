package com.jobhunter.repository;

import com.jobhunter.model.entity.SearchRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchRunRepository extends JpaRepository<SearchRun, UUID> {
    List<SearchRun> findTop10ByOrderByStartedAtDesc();
    Page<SearchRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
