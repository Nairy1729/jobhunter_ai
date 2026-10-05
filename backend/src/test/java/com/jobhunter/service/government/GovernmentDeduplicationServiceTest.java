package com.jobhunter.service.government;

import com.jobhunter.model.entity.government.CorrigendumType;
import com.jobhunter.model.entity.government.GovernmentJob;
import com.jobhunter.model.entity.government.GovernmentJobCorrigendum;
import com.jobhunter.model.entity.government.GovernmentJobStatus;
import com.jobhunter.repository.government.GovernmentJobCorrigendumRepository;
import com.jobhunter.repository.government.GovernmentJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GovernmentDeduplicationServiceTest {

    private GovernmentDeduplicationService deduplicationService;
    private GovernmentJobRepository jobRepository;
    private GovernmentJobCorrigendumRepository corrigendumRepository;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(GovernmentJobRepository.class);
        corrigendumRepository = Mockito.mock(GovernmentJobCorrigendumRepository.class);
        deduplicationService = new GovernmentDeduplicationService(jobRepository, corrigendumRepository);
    }

    @Test
    @DisplayName("Canonical ID generation produces consistent identical hashes for duplicate notices")
    void testDeterministicCanonicalId() {
        String id1 = deduplicationService.generateCanonicalId(
                "Women & Child Development",
                "Advt. 04/2026",
                "Anganwadi Worker",
                "Uttar Pradesh",
                "Kannauj"
        );

        String id2 = deduplicationService.generateCanonicalId(
                "Women & Child Development",
                "Advt. 04/2026",
                "Anganwadi Worker",
                "Uttar Pradesh",
                "Kannauj"
        );

        assertNotNull(id1);
        assertEquals(id1, id2);
        assertTrue(id1.startsWith("gov-"));
    }

    @Test
    @DisplayName("Corrigendum updates existing canonical job deadline and status without creating duplicate job")
    void testApplyCorrigendum() {
        GovernmentJob existingJob = new GovernmentJob();
        existingJob.setCanonicalId("gov-delhi-mcd-je-civil-contractual-2026");
        existingJob.setStatus(GovernmentJobStatus.OPEN);
        existingJob.setApplicationLastDate(Instant.now().plus(2, ChronoUnit.DAYS));

        Instant revisedDate = Instant.now().plus(16, ChronoUnit.DAYS);
        GovernmentJobCorrigendum corrigendum = new GovernmentJobCorrigendum(
                existingJob,
                CorrigendumType.EXTENSION,
                "Corrigendum No. 01/2026: Extension of Deadline",
                "https://mcdonline.nic.in/corrigendum.pdf",
                Instant.now(),
                "Deadline extended by 14 days",
                revisedDate,
                null
        );

        when(jobRepository.save(any(GovernmentJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GovernmentJob updated = deduplicationService.applyCorrigendum(existingJob, corrigendum);

        verify(corrigendumRepository, times(1)).save(corrigendum);
        assertEquals(revisedDate, updated.getApplicationLastDate());
        assertEquals(GovernmentJobStatus.OPEN, updated.getStatus());
    }
}
