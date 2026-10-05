package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.GovernmentSource;
import com.jobhunter.model.entity.government.GovernmentSourcePriority;
import com.jobhunter.model.entity.government.GovernmentSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentSourceRepository extends JpaRepository<GovernmentSource, UUID> {

    Optional<GovernmentSource> findByOfficialDomain(String officialDomain);

    Optional<GovernmentSource> findByUrl(String url);

    List<GovernmentSource> findByActiveTrue();

    List<GovernmentSource> findBySourceTypeAndActiveTrue(GovernmentSourceType sourceType);

    List<GovernmentSource> findByStateIgnoreCaseAndActiveTrue(String state);

    List<GovernmentSource> findByPriority(GovernmentSourcePriority priority);

    long countByActiveTrue();

    long countByStatus(String status);

    long countBySourceType(GovernmentSourceType sourceType);

    @Query("SELECT s.sourceType, COUNT(s) FROM GovernmentSource s GROUP BY s.sourceType")
    List<Object[]> countGroupBySourceType();
}
