package com.jobhunter.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResumeAiOrchestratorServiceTest {

    private CandidateProfileRepository profileRepository;
    private CandidateSkillRepository candidateSkillRepository;
    private CandidateExperienceRepository experienceRepository;
    private CandidateProjectRepository projectRepository;
    private SkillRepository skillRepository;
    private ObjectMapper objectMapper;
    private WebClient.Builder webClientBuilder;

    private ResumeAiOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(CandidateProfileRepository.class);
        candidateSkillRepository = mock(CandidateSkillRepository.class);
        experienceRepository = mock(CandidateExperienceRepository.class);
        projectRepository = mock(CandidateProjectRepository.class);
        skillRepository = mock(SkillRepository.class);
        objectMapper = new ObjectMapper();
        webClientBuilder = WebClient.builder();

        when(skillRepository.findByNameIgnoreCase(any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return Optional.of(new Skill(name, "TECHNICAL", "[]"));
        });

        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        orchestratorService = new ResumeAiOrchestratorService(
                profileRepository,
                candidateSkillRepository,
                experienceRepository,
                projectRepository,
                skillRepository,
                objectMapper,
                webClientBuilder,
                "", // No API key for deterministic test
                "gemini-1.5-flash"
        );
    }

    @Test
    @DisplayName("Should extract contact info, skills, experience, and auto-fill candidate profile")
    void testExtractAndAutoFillProfile() {
        String resumeText = """
                ALEX CHEN
                Bengaluru, India | +91 98765 43210 | alex.chen@example.com
                linkedin.com/in/alexchen-dev | github.com/alexchen-cloud

                PROFESSIONAL SUMMARY
                Passionate Senior Backend Engineer with 4 years of experience architecting resilient distributed systems,
                high-throughput REST microservices, and high-performance database architectures using Spring Boot and Java.

                TECHNICAL SKILLS
                Languages: Java, SQL, Python, TypeScript
                Frameworks: Spring Boot, Spring Security, REST APIs, Microservices
                Databases: PostgreSQL, Redis, MongoDB
                Cloud & DevOps: Docker, Kubernetes, AWS, CI/CD, Git

                PROFESSIONAL EXPERIENCE
                Razorpay — Senior Software Engineer
                2022 - Present | Bengaluru, India
                - Architected high-throughput payment reconciliation microservice using Java and Spring Boot, reducing batch latency by 45%.
                - Optimized PostgreSQL indexing and query execution plans for sub-20ms transactional latency across 10M daily events.
                - Designed resilient Kafka distributed messaging pipelines with idempotency guarantees.

                FinTech Labs — Backend Engineer
                2020 - 2022 | Bengaluru, India
                - Engineered secure RESTful APIs using Spring Boot, PostgreSQL, and Redis caching.
                - Containerized microservices using Docker and deployed on Kubernetes clusters on AWS.

                TECHNICAL PROJECTS
                Distributed Ledger Engine
                - Built a distributed transaction ledger using Java, Spring Boot, and PostgreSQL with strict ACID guarantees.
                - Scaled system to process 5,000 transactions per second with sub-50ms latency.
                """;

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alex.chen@example.com");
        user.setFirstName("Alex");
        user.setLastName("Chen");

        CandidateProfile profile = new CandidateProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setHeadline("Default Headline");
        profile.setYearsOfExperience(BigDecimal.ZERO);

        ResumeAiOrchestratorService.ExtractionResult result = orchestratorService.orchestrateAndAutoFill(
                profile, resumeText, "alex_chen_resume.pdf"
        );

        assertNotNull(result);
        assertEquals("DETERMINISTIC_NLP", result.extractionSource);

        // Verify headline & location
        assertNotNull(profile.getHeadline());
        assertTrue(profile.getHeadline().contains("Senior") || profile.getHeadline().contains("Engineer"));
        assertEquals("Bengaluru, India", profile.getCurrentLocation());

        // Verify contact info
        assertEquals("+91 98765 43210", profile.getPhoneNumber());
        assertTrue(profile.getLinkedinUrl().contains("alexchen-dev"));
        assertTrue(profile.getGithubUrl().contains("alexchen-cloud"));

        // Verify years of experience
        assertTrue(profile.getYearsOfExperience().compareTo(BigDecimal.valueOf(2.0)) >= 0);

        // Verify skills detected
        assertFalse(result.skills.isEmpty());
        assertTrue(result.skills.stream().anyMatch(s -> s.name.equalsIgnoreCase("Java")));
        assertTrue(result.skills.stream().anyMatch(s -> s.name.equalsIgnoreCase("Spring Boot")));
        assertTrue(result.skills.stream().anyMatch(s -> s.name.equalsIgnoreCase("PostgreSQL")));
        assertTrue(result.skills.stream().anyMatch(s -> s.name.equalsIgnoreCase("Docker")));

        // Verify experiences parsed
        assertFalse(result.experiences.isEmpty());
        assertEquals(2, result.experiences.size());
        assertTrue(result.experiences.get(0).company.contains("Razorpay"));

        // Verify persistence calls
        verify(profileRepository, atLeastOnce()).save(profile);
        verify(candidateSkillRepository, times(1)).saveAll(anyList());
        verify(experienceRepository, times(1)).saveAll(anyList());
        verify(projectRepository, times(1)).saveAll(anyList());
    }
}
