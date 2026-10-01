package com.jobhunter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.entity.*;
import com.jobhunter.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import com.jobhunter.service.discovery.DeduplicationService;
import java.time.LocalDate;
import java.util.Map;

@Service
public class DataSeederService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final DeduplicationService deduplicationService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeederService(UserRepository userRepository,
                             CandidateProfileRepository profileRepository,
                             SkillRepository skillRepository,
                             CandidateSkillRepository candidateSkillRepository,
                             CandidateExperienceRepository experienceRepository,
                             CandidateProjectRepository projectRepository,
                             CompanyRepository companyRepository,
                             JobSourceRepository jobSourceRepository,
                             JobRepository jobRepository,
                             ResumeRepository resumeRepository,
                             DeduplicationService deduplicationService,
                             PasswordEncoder passwordEncoder,
                             ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.deduplicationService = deduplicationService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String defaultEmail = "candidate@jobhunter.ai";

        final User user = userRepository.findByEmail(defaultEmail).orElseGet(() -> {
            log.info("Creating candidate user: {}", defaultEmail);
            User newUser = new User(
                    defaultEmail,
                    passwordEncoder.encode("password123"),
                    "Alex",
                    "Engineer"
            );
            return userRepository.save(newUser);
        });
        user.setPasswordHash(passwordEncoder.encode("password123"));
        userRepository.save(user);

        CandidateProfile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            log.info("Creating candidate profile for: {}", defaultEmail);
            CandidateProfile newProfile = new CandidateProfile();
            newProfile.setUser(user);
            newProfile.setHeadline("Software Engineer / Backend & Full Stack Engineer");
            newProfile.setSummary("Software Engineer with 2.5 years of hands-on commercial experience primarily in Java, Spring Boot, React, and PostgreSQL. Experienced in developing secure RESTful APIs with Spring Security and JWT, relational data modeling, and containerized deployment with Docker.");
            newProfile.setYearsOfExperience(new BigDecimal("2.5"));
            newProfile.setCurrentLocation("India");
            newProfile.setMinSalaryInr(new BigDecimal("1000000.00")); // 10+ LPA
            newProfile.setCurrency("INR");
            newProfile.setGithubUrl("https://github.com/candidate-dev");
            newProfile.setLinkedinUrl("https://linkedin.com/in/candidate-dev");
            newProfile.setPhoneNumber("+91 98765 43210");
            try {
                newProfile.setPreferredLocations(objectMapper.writeValueAsString(List.of("Bangalore", "Hyderabad", "Pune", "Remote", "International")));
                newProfile.setWorkModes(objectMapper.writeValueAsString(List.of("REMOTE", "HYBRID", "RELOCATION")));
                newProfile.setTargetRoles(objectMapper.writeValueAsString(List.of(
                        "Software Engineer", "Backend Engineer", "Full Stack Engineer", "Java Developer", "Spring Boot Developer"
                )));
            } catch (Exception ignored) {}
            return profileRepository.save(newProfile);
        });

        // 1. Seed candidate commercial experiences if absent
        if (experienceRepository.findByCandidateProfileId(profile.getId()).isEmpty()) {
            log.info("Seeding candidate professional experiences...");
            CandidateExperience exp1 = new CandidateExperience(
                    profile,
                    "FinTech SaaS Solutions",
                    "Software Engineer",
                    "Jan 2024 - Present (1.5 years)",
                    "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Spring Security\", \"JWT\", \"Docker\", \"REST APIs\"]",
                    "[\"Engineered high-reliability transaction reconciliation REST APIs\", \"Designed PostgreSQL schema with composite B-Tree indexes\", \"Configured stateless Spring Security JWT authentication filter chain\"]",
                    "[\"Implemented payment idempotency mechanism preventing duplicate charges\", \"Reduced batch report generation latency from 14s to 1.8s through query plan optimization\"]",
                    "Financial Technology & Billing",
                    "Commercial production software engineering."
            );
            experienceRepository.save(exp1);

            CandidateExperience exp2 = new CandidateExperience(
                    profile,
                    "CloudScale Technologies",
                    "Associate Software Engineer",
                    "Jul 2023 - Dec 2023 (6 months)",
                    "[\"Java\", \"Spring Boot\", \"React\", \"SQL\", \"Git\", \"Docker\"]",
                    "[\"Developed microservices for client reporting dashboard\", \"Built responsive React interface components\", \"Maintained Docker dev environments\"]",
                    "[\"Delivered automated Swagger/OpenAPI documentation for 20+ endpoints\", \"Participated in peer code reviews maintaining 90%+ test coverage\"]",
                    "Enterprise B2B SaaS",
                    "Commercial employment records."
            );
            experienceRepository.save(exp2);
        }

        // 2. Seed candidate projects if absent
        if (projectRepository.findByCandidateProfileId(profile.getId()).isEmpty()) {
            log.info("Seeding candidate technical projects...");
            CandidateProject proj1 = new CandidateProject(
                    profile,
                    "Mentor-Mentee Collaboration Platform",
                    "Full-stack mentoring and code review platform connecting senior and junior engineers.",
                    "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"React\", \"TypeScript\", \"WebSocket\", \"Docker\"]",
                    "Layered microservice architecture with Spring Boot REST backend, STOMP WebSocket messaging, and React SPA frontend.",
                    "[\"Designed normalized PostgreSQL database schema with Flyway migrations\", \"Implemented WebSocket notification service for real-time mentorship updates\", \"Authored integration tests using Testcontainers\"]",
                    "[\"Sustained 100+ concurrent simulated sessions without message loss\", \"Maintained sub-50ms REST API response times on all primary paths\"]",
                    "Public GitHub repository with automated GitHub Actions CI pipeline.",
                    "https://github.com/candidate-dev/mentor-mentee-platform"
            );
            projectRepository.save(proj1);

            CandidateProject proj2 = new CandidateProject(
                    profile,
                    "Distributed Task Queue & Scheduler",
                    "Resilient distributed task coordinator ensuring exactly-once execution semantics.",
                    "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Redis\"]",
                    "Distributed leader election using PostgreSQL advisory locks and Redis key-value caching.",
                    "[\"Engineered exponential backoff retry policy for failing tasks\", \"Designed transactional outbox pattern for task status dispatch\"]",
                    "[\"Verified exactly-once execution across 10,000 synthetic task dispatches with zero duplicates\"]",
                    "Open-source implementation and architectural design RFC.",
                    "https://github.com/candidate-dev/distributed-task-scheduler"
            );
            projectRepository.save(proj2);
        }

        // 3. Ensure Skills have proper experienceType (Commercial vs Project-Only)
        updateOrAddSkill(profile, "Java", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Commercial backend development at FinTech SaaS Solutions");
        updateOrAddSkill(profile, "Spring Boot", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Production microservices, Spring Data JPA, Spring Security at FinTech SaaS");
        updateOrAddSkill(profile, "Spring Security", "INTERMEDIATE", new BigDecimal("2.0"), true, "COMMERCIAL", new BigDecimal("0.95"), "Stateless JWT filter chains, role-based method authorization");
        updateOrAddSkill(profile, "PostgreSQL", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Schema normalization, ACID transactions, B-Tree index optimization");
        updateOrAddSkill(profile, "SQL", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Complex joins, subqueries, EXPLAIN ANALYZE execution profiling");
        updateOrAddSkill(profile, "REST APIs", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "RFC 7807 problem details, HTTP status code discipline, OpenAPI specs");
        updateOrAddSkill(profile, "JWT", "ADVANCED", new BigDecimal("2.0"), true, "COMMERCIAL", new BigDecimal("0.95"), "Token generation, claims extraction, HMAC-SHA signing");
        updateOrAddSkill(profile, "React", "INTERMEDIATE", new BigDecimal("1.5"), true, "COMMERCIAL", new BigDecimal("0.85"), "Single page applications, React Hooks, Tailwind CSS");
        updateOrAddSkill(profile, "Node.js", "INTERMEDIATE", new BigDecimal("1.5"), true, "COMMERCIAL", new BigDecimal("0.80"), "Express.js REST APIs, asynchronous event loop");
        updateOrAddSkill(profile, "Docker", "INTERMEDIATE", new BigDecimal("1.0"), false, "COMMERCIAL", new BigDecimal("0.85"), "Dockerfile authoring, containerizing Spring Boot and React services");
        updateOrAddSkill(profile, "Git", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Branching strategies, PR code reviews, rebasing");
        updateOrAddSkill(profile, "Microservices", "ADVANCED", new BigDecimal("2.5"), true, "COMMERCIAL", new BigDecimal("1.00"), "Production microservice architecture in Spring Boot");

        // Project-only skills (Critical: explicitly distinguished from commercial)
        updateOrAddSkill(profile, "WebSocket", "INTERMEDIATE", new BigDecimal("1.0"), false, "PROJECT_ONLY", new BigDecimal("0.80"), "Implemented real-time notifications in Mentor-Mentee Platform project");
        updateOrAddSkill(profile, "TypeScript", "INTERMEDIATE", new BigDecimal("1.0"), false, "PROJECT_ONLY", new BigDecimal("0.80"), "Used for frontend client types in Mentor-Mentee Platform project");

        // 4. Seed Candidate Master Resume if absent
        if (resumeRepository.findByCandidateProfileId(profile.getId()).isEmpty()) {
            log.info("Seeding candidate master resume...");
            Resume masterResume = new Resume();
            masterResume.setCandidateProfile(profile);
            masterResume.setTitle("Alex_Dev_Master_Resume.pdf");
            masterResume.setFilePath("storage/resumes/Alex_Dev_Master_Resume.pdf");
            masterResume.setFileType("PDF");
            masterResume.setFileSizeBytes(1048576L);
            masterResume.setMaster(true);
            masterResume.setRawExtractedText("""
                    Alex Dev
                    Software Engineer / Backend & Full Stack Engineer
                    Email: candidate@jobhunter.ai | Phone: +91 98765 43210 | Location: Bengaluru, India
                    GitHub: https://github.com/candidate-dev | LinkedIn: https://linkedin.com/in/candidate-dev

                    SUMMARY:
                    Software Engineer with 2.5 years of hands-on commercial experience primarily in Java, Spring Boot, React, and PostgreSQL. Experienced in developing secure RESTful APIs with Spring Security and JWT, relational data modeling, and containerized deployment with Docker.

                    TECHNICAL SKILLS:
                    Languages & Core: Java, SQL, TypeScript, Git
                    Frameworks & Backend: Spring Boot, Spring Security, JWT, REST APIs, Node.js, React
                    Databases & Tools: PostgreSQL, Docker, Redis, WebSocket

                    PROFESSIONAL EXPERIENCE:
                    FinTech SaaS Solutions — Software Engineer (Jan 2024 - Present)
                    - Engineered high-reliability transaction reconciliation REST APIs.
                    - Designed PostgreSQL schema with composite B-Tree indexes, reducing batch report generation latency from 14s to 1.8s.
                    - Configured stateless Spring Security JWT authentication filter chain.
                    - Implemented payment idempotency mechanism preventing duplicate charges.

                    CloudScale Technologies — Associate Software Engineer (Jul 2023 - Dec 2023)
                    - Developed microservices for client reporting dashboard using Java and Spring Boot.
                    - Built responsive React interface components.
                    - Maintained Docker dev environments and delivered automated Swagger/OpenAPI documentation for 20+ endpoints.

                    PROJECTS:
                    Mentor-Mentee Collaboration Platform (Java, Spring Boot, PostgreSQL, React, TypeScript, WebSocket, Docker)
                    - Designed normalized PostgreSQL database schema with Flyway migrations.
                    - Implemented WebSocket notification service for real-time mentorship updates.
                    - Sustained 100+ concurrent simulated sessions without message loss.

                    Distributed Task Queue & Scheduler (Java, Spring Boot, PostgreSQL, Redis)
                    - Engineered exponential backoff retry policy for failing tasks.
                    - Verified exactly-once execution across 10,000 synthetic task dispatches.

                    EDUCATION:
                    Bachelor of Technology in Computer Science & Engineering (2019 - 2023)
                    """);
            try {
                masterResume.setStructuredContent(objectMapper.writeValueAsString(Map.of(
                        "title", "Alex_Dev_Master_Resume.pdf",
                        "detectedSkills", List.of("Java", "Spring Boot", "PostgreSQL", "SQL", "Spring Security", "JWT", "Docker", "Git", "React", "TypeScript")
                )));
            } catch (Exception ignored) {}
            resumeRepository.save(masterResume);
        }

        seedBenchmarkJobs();

        log.info("Candidate profile, experiences, projects, skills, master resume, and benchmark jobs fully synchronized!");
    }

    private void seedBenchmarkJobs() {
        JobSource leverSource = jobSourceRepository.findByName("LEVER")
                .orElseGet(() -> jobSourceRepository.save(new JobSource("LEVER", "LEVER", "https://jobs.lever.co", 20)));

        // 1. Strong Match Benchmark Job (Targeting Alex's Java + Spring Boot + PostgreSQL + Docker stack, 2.5 YOE)
        Company fintechCo = companyRepository.findByNameIgnoreCase("FinTech Solutions")
                .orElseGet(() -> {
                    Company c = new Company("FinTech Solutions");
                    c.setDomain("fintechsolutions.io");
                    c.setIndustry("FinTech");
                    c.setCareerPageUrl("https://fintechsolutions.io/careers");
                    return companyRepository.save(c);
                });

        String strongUrl = "https://jobs.lever.co/fintechsolutions/software-engineer-java";
        String strongHash = deduplicationService.generateCanonicalUrlHash(strongUrl);
        if (!jobRepository.existsByCanonicalUrlHash(strongHash)) {
            Job strongJob = new Job();
            strongJob.setCompany(fintechCo);
            strongJob.setJobSource(leverSource);
            strongJob.setTitle("Software Engineer (Java / Spring Boot)");
            strongJob.setNormalizedTitle("Software Engineer");
            strongJob.setDepartment("Core Engineering");
            strongJob.setLocation("Bengaluru, India");
            strongJob.setWorkMode("HYBRID");
            strongJob.setEmploymentType("FULL_TIME");
            strongJob.setMinExperienceYears(new BigDecimal("2.0"));
            strongJob.setMaxExperienceYears(new BigDecimal("4.0"));
            strongJob.setMinSalary(new BigDecimal("1800000"));
            strongJob.setMaxSalary(new BigDecimal("2600000"));
            strongJob.setSalaryCurrency("INR");
            strongJob.setJobUrl(strongUrl);
            strongJob.setCanonicalUrl(strongUrl);
            strongJob.setCanonicalUrlHash(strongHash);
            strongJob.setContentHash(deduplicationService.generateContentHash("Software Engineer (Java / Spring Boot)", "FinTech Solutions", "Core Banking microservices"));
            strongJob.setPostingDate(LocalDate.now().minusDays(3));
            strongJob.setRawDescriptionMarkdown("""
                    ## Role Summary
                    FinTech Solutions is seeking a Software Engineer (Java / Spring Boot) to develop high-throughput transaction microservices for our core banking platform.

                    ### Responsibilities
                    * Build and maintain resilient REST microservices using Java and Spring Boot.
                    * Design relational database schemas and optimize query performance in PostgreSQL.
                    * Implement secure API authentication using Spring Security and JWT.
                    * Containerize backend applications using Docker for deployment.
                    * Participate in code reviews, automated CI/CD testing, and agile sprints.

                    ### Qualifications & Requirements
                    * 2+ years of hands-on commercial experience in backend engineering.
                    * Strong proficiency in Java 17 and Spring Boot microservice architectures.
                    * Solid working experience with PostgreSQL, ACID transactions, and SQL query tuning.
                    * Experience securing RESTful APIs with Spring Security and JWT token mechanisms.
                    * Experience with Docker and containerization best practices.
                    * Version control discipline with Git and automated CI/CD workflows.
                    * Nice to have: Familiarity with React or frontend client communication.
                    """);
            try {
                strongJob.setStructuredJobSpec(objectMapper.writeValueAsString(Map.of(
                        "detectedTechnologies", List.of("Java", "Spring Boot", "PostgreSQL", "SQL", "Spring Security", "JWT", "Docker", "Git", "REST APIs"),
                        "experienceYears", 2.0,
                        "workMode", "HYBRID"
                )));
            } catch (Exception ignored) {}
            jobRepository.save(strongJob);
            log.info("Seeded Strong Match Benchmark Job: FinTech Solutions - Software Engineer");
        }

        // 2. Poor Match Benchmark Job (Senior iOS Swift / Objective-C requiring 5+ YOE)
        Company bytedanceCo = companyRepository.findByNameIgnoreCase("ByteDance")
                .orElseGet(() -> {
                    Company c = new Company("ByteDance");
                    c.setDomain("bytedance.com");
                    c.setIndustry("Consumer Tech");
                    c.setCareerPageUrl("https://careers.bytedance.com");
                    return companyRepository.save(c);
                });

        String poorUrl = "https://careers.bytedance.com/job/senior-ios-engineer-101";
        String poorHash = deduplicationService.generateCanonicalUrlHash(poorUrl);
        if (!jobRepository.existsByCanonicalUrlHash(poorHash)) {
            Job poorJob = new Job();
            poorJob.setCompany(bytedanceCo);
            poorJob.setJobSource(leverSource);
            poorJob.setTitle("Senior iOS Engineer (Swift / Objective-C)");
            poorJob.setNormalizedTitle("Senior iOS Engineer");
            poorJob.setDepartment("Mobile Product");
            poorJob.setLocation("Singapore");
            poorJob.setWorkMode("ON_SITE");
            poorJob.setEmploymentType("FULL_TIME");
            poorJob.setMinExperienceYears(new BigDecimal("5.0"));
            poorJob.setMaxExperienceYears(new BigDecimal("8.0"));
            poorJob.setMinSalary(new BigDecimal("120000"));
            poorJob.setMaxSalary(new BigDecimal("180000"));
            poorJob.setSalaryCurrency("SGD");
            poorJob.setJobUrl(poorUrl);
            poorJob.setCanonicalUrl(poorUrl);
            poorJob.setCanonicalUrlHash(poorHash);
            poorJob.setContentHash(deduplicationService.generateContentHash("Senior iOS Engineer (Swift / Objective-C)", "ByteDance", "Mobile iOS client"));
            poorJob.setPostingDate(LocalDate.now().minusDays(5));
            poorJob.setRawDescriptionMarkdown("""
                    ## Role Summary
                    ByteDance is looking for a Senior iOS Engineer to engineer smooth, high-fidelity mobile experiences for video rendering and social feed interactions.

                    ### Responsibilities
                    * Architect native iOS applications using Swift and legacy Objective-C modules.
                    * Profile memory, battery consumption, and rendering latency with Instruments.
                    * Implement reactive mobile user interfaces using SwiftUI and UIKit.
                    * Maintain camera and hardware pipeline integration using AVFoundation.

                    ### Qualifications & Requirements
                    * 5+ years of dedicated professional native iOS development experience.
                    * Deep mastery of Swift, Objective-C, Cocoa Touch, and iOS runtime internals.
                    * Expert-level understanding of Automatic Reference Counting (ARC) and memory management.
                    * Production track record releasing consumer applications on the Apple App Store.
                    * Experience with iOS multimedia streaming, AVFoundation, and Core Animation.
                    """);
            try {
                poorJob.setStructuredJobSpec(objectMapper.writeValueAsString(Map.of(
                        "detectedTechnologies", List.of("Swift", "Objective-C", "iOS", "SwiftUI", "UIKit", "Xcode", "AVFoundation"),
                        "experienceYears", 5.0,
                        "workMode", "ON_SITE"
                )));
            } catch (Exception ignored) {}
            jobRepository.save(poorJob);
            log.info("Seeded Poor Match Benchmark Job: ByteDance - Senior iOS Engineer");
        }
    }

    private void updateOrAddSkill(CandidateProfile profile, String skillName, String level, BigDecimal years,
                                 boolean primary, String expType, BigDecimal confidence, String evidence) {
        Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                .orElseGet(() -> skillRepository.save(new Skill(skillName, "TECHNICAL", "[]")));

        CandidateSkill candidateSkill = candidateSkillRepository
                .findByCandidateProfileIdAndSkillId(profile.getId(), skill.getId())
                .orElseGet(() -> new CandidateSkill(profile, skill, level, years, primary, evidence));

        candidateSkill.setProficiencyLevel(level);
        candidateSkill.setYearsExperience(years);
        candidateSkill.setPrimary(primary);
        candidateSkill.setExperienceType(expType);
        candidateSkill.setConfidence(confidence);
        candidateSkill.setEvidenceSource(evidence);
        candidateSkill.setEvidenceText(evidence);

        candidateSkillRepository.save(candidateSkill);
    }
}
