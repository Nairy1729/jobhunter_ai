package com.jobhunter.service.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.dto.matching.MatchAnalysisResponse;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import com.jobhunter.service.embedding.DeterministicLocalEmbeddingProvider;
import com.jobhunter.service.embedding.EmbeddingService;
import com.jobhunter.service.embedding.GeminiEmbeddingProvider;
import com.jobhunter.service.embedding.VectorService;
import com.jobhunter.service.prioritization.JobPrioritizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SemanticMatchingServiceTest {

    private JobRepository jobRepository;
    private CandidateProfileRepository profileRepository;
    private JobMatchRepository jobMatchRepository;
    private AgentRunRepository agentRunRepository;
    private JobRequirementExtractor requirementExtractor;
    private VectorService vectorService;
    private GroundingVerificationGate groundingGate;
    private ObjectMapper objectMapper;

    private SemanticMatchingService matchingService;

    private CandidateProfile candidateProfile;
    private UUID candidateUserId;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
        profileRepository = Mockito.mock(CandidateProfileRepository.class);
        jobMatchRepository = Mockito.mock(JobMatchRepository.class);
        agentRunRepository = Mockito.mock(AgentRunRepository.class);
        SkillRepository skillRepository = Mockito.mock(SkillRepository.class);
        JobRequirementRepository requirementRepository = Mockito.mock(JobRequirementRepository.class);

        when(skillRepository.findAll()).thenReturn(List.of(
                new Skill("Java", "LANGUAGE", "[]"),
                new Skill("Spring Boot", "FRAMEWORK", "[]"),
                new Skill("PostgreSQL", "DATABASE", "[]"),
                new Skill("Docker", "DEVOPS", "[]")
        ));

        requirementExtractor = new JobRequirementExtractor(requirementRepository, skillRepository);
        groundingGate = new GroundingVerificationGate();
        objectMapper = new ObjectMapper();

        DeterministicLocalEmbeddingProvider localProvider = new DeterministicLocalEmbeddingProvider();
        GeminiEmbeddingProvider mockGemini = Mockito.mock(GeminiEmbeddingProvider.class);
        when(mockGemini.isAvailable()).thenReturn(false);

        EmbeddingService embeddingService = new EmbeddingService(mockGemini, localProvider, "deterministic");
        JobEmbeddingRepository jobEmbeddingRepo = Mockito.mock(JobEmbeddingRepository.class);
        CandidateEmbeddingRepository candEmbeddingRepo = Mockito.mock(CandidateEmbeddingRepository.class);

        vectorService = new VectorService(null, jobEmbeddingRepo, candEmbeddingRepo, embeddingService, objectMapper);

        JobPrioritizationService prioritizationService = new com.jobhunter.service.prioritization.JobPrioritizationService();

        matchingService = new SemanticMatchingService(
                jobRepository,
                profileRepository,
                jobMatchRepository,
                agentRunRepository,
                requirementExtractor,
                vectorService,
                groundingGate,
                prioritizationService,
                objectMapper
        );

        // Candidate Profile Setup: 2.5 YOE, Java, Spring Boot, PostgreSQL, Docker
        candidateUserId = UUID.randomUUID();
        User user = new User("alex@jobhunter.ai", "hash", "Alex", "Dev");
        user.setId(candidateUserId);

        candidateProfile = new CandidateProfile();
        candidateProfile.setId(UUID.randomUUID());
        candidateProfile.setUser(user);
        candidateProfile.setHeadline("Software Engineer");
        candidateProfile.setYearsOfExperience(new BigDecimal("2.5"));
        candidateProfile.setCurrentLocation("Bangalore, India");
        candidateProfile.setPreferredLocations("[\"Bangalore\", \"Remote\"]");
        candidateProfile.setWorkModes("[\"REMOTE\", \"HYBRID\"]");

        List<CandidateSkill> skills = new ArrayList<>();
        skills.add(new CandidateSkill(candidateProfile, new Skill("Java", "LANGUAGE", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "Core Java and backend"));
        skills.add(new CandidateSkill(candidateProfile, new Skill("Spring Boot", "FRAMEWORK", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "Production REST APIs"));
        skills.add(new CandidateSkill(candidateProfile, new Skill("PostgreSQL", "DATABASE", "[]"), "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Work", "Relational schema and indexing"));
        skills.add(new CandidateSkill(candidateProfile, new Skill("Docker", "DEVOPS", "[]"), "INTERMEDIATE", new BigDecimal("1.0"), false, "COMMERCIAL", new BigDecimal("0.85"), "Work", "Docker Compose"));
        candidateProfile.setSkills(skills);

        when(requirementRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requirementRepository.findByJobIdOrderByInferredImportanceDesc(any())).thenReturn(Collections.emptyList());
        when(profileRepository.findByUserId(candidateUserId)).thenReturn(Optional.of(candidateProfile));
        when(jobMatchRepository.save(any(JobMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Case 1: Strong Match — Job aligns directly with Java, Spring Boot, and 2-3 YOE")
    void testStrongMatch() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Backend Engineer - Spring Boot");
        job.setCompany(new Company("GrowthSaaS"));
        job.setLocation("Remote");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("2.0"));
        job.setMaxExperienceYears(new BigDecimal("4.0"));
        job.setRawDescriptionMarkdown("""
            ## Backend Engineer
            We are looking for a Backend Engineer with 2+ years experience building APIs in Java and Spring Boot.
            Must be proficient with PostgreSQL and Docker.
            """);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        MatchAnalysisResponse response = matchingService.analyzeAndMatch(jobId, candidateUserId);

        assertNotNull(response);
        assertEquals("APPLY", response.getRecommendation());
        assertEquals("HIGH_PRIORITY", response.getQueueTier());
        assertTrue(response.getPriorityScore().doubleValue() >= 75.0, "Strong match score should be >= 75.0, got: " + response.getPriorityScore());
        assertFalse(response.getStrongMatches().isEmpty());
        assertTrue(response.getStrongMatches().stream().anyMatch(s -> s.contains("Java")));
        assertNotNull(response.getAdvantageReport());
        assertFalse(response.getAdvantageReport().getWhatToEmphasize().isEmpty());
    }

    @Test
    @DisplayName("Case 2: Partial Match — Good backend fit but has Gaps in Kafka and AWS")
    void testPartialMatchWithGaps() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Senior Backend Engineer");
        job.setCompany(new Company("CloudTech"));
        job.setLocation("Bangalore, India");
        job.setWorkMode("HYBRID");
        job.setMinExperienceYears(new BigDecimal("4.0"));
        job.setMaxExperienceYears(new BigDecimal("6.0"));
        job.setRawDescriptionMarkdown("""
            ## Senior Backend Engineer
            Required:
            * Strong Java and Spring Boot skills.
            * Hands-on experience with Kafka and AWS cloud deployment.
            """);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        MatchAnalysisResponse response = matchingService.analyzeAndMatch(jobId, candidateUserId);

        assertNotNull(response);
        assertEquals("APPLY_AFTER_TAILORING", response.getRecommendation());
        assertFalse(response.getGaps().isEmpty(), "Should identify Kafka and AWS as gaps");
        assertTrue(response.getGaps().stream().anyMatch(g -> g.contains("Kafka") || g.contains("AWS")));
    }

    @Test
    @DisplayName("Case 3: Poor Match / Do Not Apply — 10+ years required, completely foreign stack")
    void testPoorMatchDoNotApply() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Principal Graphics Engine Architect");
        job.setCompany(new Company("GameDev Studios"));
        job.setLocation("Tokyo, Japan");
        job.setWorkMode("ON_SITE");
        job.setMinExperienceYears(new BigDecimal("10.0"));
        job.setRawDescriptionMarkdown("""
            ## Principal Graphics Architect
            Requirements:
            * 10+ years C++ 3D rendering engine development (DirectX 12, Vulkan).
            * Low-level GPU memory optimization.
            """);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        MatchAnalysisResponse response = matchingService.analyzeAndMatch(jobId, candidateUserId);

        assertNotNull(response);
        assertEquals("DO_NOT_APPLY", response.getRecommendation());
        assertTrue(response.getPriorityScore().doubleValue() < 40.0, "Poor match score should be < 40.0, got: " + response.getPriorityScore());
        assertFalse(response.getRiskFactors().isEmpty(), "Should flag huge seniority and work mode delta as risk factors");
    }

    @Test
    @DisplayName("Should handle missing salary and missing experience without throwing exceptions or fabricating values")
    void testMissingSalaryAndExperience() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Software Engineer");
        job.setCompany(new Company("StartupInc"));
        job.setLocation("Remote");
        job.setWorkMode("UNKNOWN");
        job.setMinSalary(null);
        job.setMaxSalary(null);
        job.setMinExperienceYears(null);
        job.setMaxExperienceYears(null);
        job.setRawDescriptionMarkdown("Looking for an energetic engineer to build web software.");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        MatchAnalysisResponse response = matchingService.analyzeAndMatch(jobId, candidateUserId);

        assertNotNull(response);
        assertNotNull(response.getRecommendation());
        assertNotNull(response.getPriorityScore());
    }

    @Test
    @DisplayName("The Edge Advantage Report strictly exposes the 10 application positioning dimensions")
    void testAdvantageReportTenDimensions() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Senior Java Backend Engineer");
        job.setCompany(new Company("FinTech Corp"));
        job.setLocation("Bangalore, India");
        job.setWorkMode("HYBRID");
        job.setMinExperienceYears(new BigDecimal("3.0"));
        job.setRawDescriptionMarkdown("Java, Spring Boot, PostgreSQL required.");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        var report = matchingService.getAdvantageReport(jobId, candidateUserId);

        assertNotNull(report);
        // Verify all 10 dimensions are populated and non-empty
        assertFalse(report.getEmployerPriorities().isEmpty(), "Dim 1: Employer Priorities");
        assertNotNull(report.getCandidateRelevance(), "Dim 2: Candidate Relevance");
        assertFalse(report.getStrongestEvidence().isEmpty(), "Dim 3: Strongest Evidence");
        assertFalse(report.getWhatToEmphasize().isEmpty(), "Dim 4: What to Emphasize");
        assertFalse(report.getWhatToDeemphasize().isEmpty(), "Dim 5: What to De-emphasize");
        assertFalse(report.getHonestGaps().isEmpty(), "Dim 6: Honest Gaps");
        assertFalse(report.getTransferableSkills().isEmpty(), "Dim 7: Transferable Skills");
        assertFalse(report.getResumePositioning().isEmpty(), "Dim 8: Resume Positioning");
        assertFalse(report.getApplicationFitAndPositioning().isEmpty(), "Dim 9: Application Fit & Positioning");
        assertNotNull(report.getApplicationStrategy(), "Dim 10: Application Strategy");
    }

    @Test
    @DisplayName("Tailoring Recommendations provide grounded section reordering and bullet proposals")
    void testTailoringRecommendations() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Backend Engineer");
        job.setCompany(new Company("SaaSCo"));
        job.setLocation("Remote");
        job.setWorkMode("REMOTE");
        job.setMinExperienceYears(new BigDecimal("2.0"));
        job.setRawDescriptionMarkdown("Java and Spring Boot microservices with PostgreSQL.");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        var tailoring = matchingService.getTailoringRecommendations(jobId, candidateUserId);

        assertNotNull(tailoring);
        assertEquals(jobId, tailoring.getJobId());
        assertFalse(tailoring.getSectionsToReorder().isEmpty());
        assertFalse(tailoring.getSkillsToFeature().isEmpty());
        assertFalse(tailoring.getSkillsToDeemphasize().isEmpty());
        assertFalse(tailoring.getBulletSharpeningProposals().isEmpty());
        assertNotNull(tailoring.getRationale());
    }

    @Test
    @DisplayName("Milestone 6: Direct match with matching location & stack achieves HIGH_PRIORITY")
    void testDirectMatchAchievesHighPriority() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setTitle("Software Engineer (Java / Spring Boot)");
        job.setCompany(new Company("FinTech Solutions"));
        job.setLocation("Bangalore");
        job.setWorkMode("HYBRID");
        job.setMinExperienceYears(new BigDecimal("2.0"));
        job.setMaxExperienceYears(new BigDecimal("4.0"));
        job.setPostingDate(java.time.LocalDate.now().minusDays(2));
        job.setRawDescriptionMarkdown("""
                FinTech Solutions is seeking a Software Engineer (Java / Spring Boot).
                * 2.0 - 4.0 years of experience
                * Java 17 and Spring Boot microservices
                * PostgreSQL database design
                """);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobMatchRepository.findByJobIdAndCandidateProfileId(jobId, candidateProfile.getId())).thenReturn(Optional.empty());

        MatchAnalysisResponse match = matchingService.analyzeAndMatch(jobId, candidateUserId);

        assertNotNull(match);
        assertEquals("HIGH_PRIORITY", match.getPriorityCategory(), "Direct match in candidate's city & stack must be HIGH_PRIORITY");
        assertEquals("NEW", match.getFreshness());
    }
}
