package com.jobhunter.repository.government;

import com.jobhunter.model.entity.government.GovernmentEmploymentType;
import com.jobhunter.model.entity.government.GovernmentJob;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.model.entity.government.GovernmentVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentJobRepository extends JpaRepository<GovernmentJob, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<GovernmentJob> {

    Optional<GovernmentJob> findByCanonicalId(String canonicalId);

    List<GovernmentJob> findByNotificationNumberIgnoreCase(String notificationNumber);

    List<GovernmentJob> findByStatusAndApplicationLastDateBefore(GovernmentJobStatus status, Instant now);

    long countByStatus(GovernmentJobStatus status);

    long countByVerificationStatus(GovernmentVerificationStatus verificationStatus);

    @Query("SELECT j.employmentType, COUNT(j) FROM GovernmentJob j GROUP BY j.employmentType")
    List<Object[]> countGroupByEmploymentType();

    @Query("SELECT j.verificationStatus, COUNT(j) FROM GovernmentJob j GROUP BY j.verificationStatus")
    List<Object[]> countGroupByVerificationStatus();
}
