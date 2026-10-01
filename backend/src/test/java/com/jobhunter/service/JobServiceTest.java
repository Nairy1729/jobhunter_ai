package com.jobhunter.service;

import com.jobhunter.model.dto.JobAppliedStatusDto;
import com.jobhunter.model.dto.JobDto;
import com.jobhunter.model.entity.Application;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Job;
import com.jobhunter.repository.ApplicationRepository;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.JobMatchRepository;
import com.jobhunter.repository.JobRepository;
import com.jobhunter.repository.SearchRunRepository;
import com.jobhunter.service.discovery.DiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SearchRunRepository searchRunRepository;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private com.jobhunter.service.discovery.HardEligibilityFilterService hardEligibilityFilter;

    @Mock
    private com.jobhunter.service.matching.UsefulnessGateService usefulnessGate;

    @Mock
    private com.jobhunter.service.matching.SemanticMatchingService semanticMatchingService;

    @Mock
    private com.jobhunter.service.profile.ProfileReadinessService profileReadinessService;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        jobService = new JobService(
                jobRepository,
                searchRunRepository,
                discoveryService,
                applicationRepository,
                candidateProfileRepository,
                jobMatchRepository,
                hardEligibilityFilter,
                usefulnessGate,
                semanticMatchingService,
                profileReadinessService,
                objectMapper
        );
    }

    @Test
    void testSetJobAppliedStatus_MarkAppliedTrue() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Job job = new Job();
        job.setId(jobId);

        CandidateProfile profile = new CandidateProfile();
        profile.setId(profileId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(applicationRepository.findByCandidateProfileIdAndJobId(profileId, jobId)).thenReturn(Optional.empty());

        JobAppliedStatusDto result = jobService.setJobAppliedStatus(jobId, userId, true);

        assertTrue(result.isApplied());
        assertNotNull(result.getAppliedAt());
        assertEquals(jobId, result.getJobId());

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        Application saved = captor.getValue();
        assertTrue(saved.isApplied());
        assertNotNull(saved.getAppliedAt());
        assertEquals(profile, saved.getCandidateProfile());
        assertEquals(job, saved.getJob());
    }

    @Test
    void testSetJobAppliedStatus_MarkAppliedFalse_Undo() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Job job = new Job();
        job.setId(jobId);

        CandidateProfile profile = new CandidateProfile();
        profile.setId(profileId);

        Application existingApp = new Application(profile, job, true);
        existingApp.setAppliedAt(Instant.now());

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(applicationRepository.findByCandidateProfileIdAndJobId(profileId, jobId)).thenReturn(Optional.of(existingApp));

        JobAppliedStatusDto result = jobService.setJobAppliedStatus(jobId, userId, false);

        assertFalse(result.isApplied());
        assertNull(result.getAppliedAt());

        verify(applicationRepository).save(existingApp);
        assertFalse(existingApp.isApplied());
        assertNull(existingApp.getAppliedAt());
    }

    @Test
    void testFindJobs_EnrichesAppliedStatus() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Job job = new Job();
        job.setId(jobId);

        CandidateProfile profile = new CandidateProfile();
        profile.setId(profileId);

        Application app = new Application(profile, job, true);
        Instant appliedTime = Instant.now().minusSeconds(3600);
        app.setAppliedAt(appliedTime);

        com.jobhunter.dto.profile.ProfileReadinessReport readyReport = new com.jobhunter.dto.profile.ProfileReadinessReport(
                com.jobhunter.dto.profile.ProfileReadinessState.PROFILE_READY,
                100,
                true,
                "PROFILE READY",
                "Ready"
        );
        when(profileReadinessService.evaluateProfile(profile)).thenReturn(readyReport);
        when(hardEligibilityFilter.evaluate(eq(job), eq(profile))).thenReturn(com.jobhunter.service.discovery.dto.HardEligibilityResult.pass());
        when(usefulnessGate.evaluateUsefulness(eq(job), eq(profile), any())).thenReturn(new com.jobhunter.dto.matching.UsefulnessDecision(true, "USEFUL", "HIGH_RELEVANCE", "Matches"));

        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(applicationRepository.findByCandidateProfileIdAndAppliedTrue(profileId)).thenReturn(List.of(app));

        when(jobRepository.findAll(any(Specification.class))).thenReturn(List.of(job));

        JobDto rawDto = new JobDto();
        rawDto.setId(jobId);
        when(discoveryService.mapToDto(job)).thenReturn(rawDto);

        Page<JobDto> resultPage = jobService.findJobs(null, null, null, null, null, null, 0, 10, userId);

        assertEquals(1, resultPage.getContent().size());
        JobDto enrichedDto = resultPage.getContent().get(0);
        assertTrue(enrichedDto.isApplied());
        assertEquals(appliedTime, enrichedDto.getAppliedAt());
    }
}
