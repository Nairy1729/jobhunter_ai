package com.jobhunter.service;

import com.jobhunter.model.dto.JobAppliedStatusDto;
import com.jobhunter.model.dto.JobDto;
import com.jobhunter.model.dto.SearchRunDto;
import com.jobhunter.model.entity.Application;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Job;
import com.jobhunter.model.entity.SearchRun;
import com.jobhunter.repository.ApplicationRepository;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.JobMatchRepository;
import com.jobhunter.repository.JobRepository;
import com.jobhunter.repository.SearchRunRepository;
import com.jobhunter.service.discovery.DiscoveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final SearchRunRepository searchRunRepository;
    private final DiscoveryService discoveryService;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final com.jobhunter.service.discovery.HardEligibilityFilterService hardEligibilityFilter;
    private final com.jobhunter.service.matching.UsefulnessGateService usefulnessGate;
    private final com.jobhunter.service.matching.SemanticMatchingService semanticMatchingService;
    private final com.jobhunter.service.profile.ProfileReadinessService profileReadinessService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public JobService(
            JobRepository jobRepository,
            SearchRunRepository searchRunRepository,
            DiscoveryService discoveryService,
            ApplicationRepository applicationRepository,
            CandidateProfileRepository candidateProfileRepository,
            JobMatchRepository jobMatchRepository,
            com.jobhunter.service.discovery.HardEligibilityFilterService hardEligibilityFilter,
            com.jobhunter.service.matching.UsefulnessGateService usefulnessGate,
            com.jobhunter.service.matching.SemanticMatchingService semanticMatchingService,
            com.jobhunter.service.profile.ProfileReadinessService profileReadinessService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.searchRunRepository = searchRunRepository;
        this.discoveryService = discoveryService;
        this.applicationRepository = applicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.hardEligibilityFilter = hardEligibilityFilter;
        this.usefulnessGate = usefulnessGate;
        this.semanticMatchingService = semanticMatchingService;
        this.profileReadinessService = profileReadinessService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<JobDto> findJobs(
            String role,
            String location,
            String workMode,
            String technology,
            String source,
            BigDecimal minSalary,
            int page,
            int size) {
        return findJobs(role, location, workMode, technology, source, minSalary, null, null, "RECOMMENDED", page, size, null);
    }

    @Transactional(readOnly = true)
    public Page<JobDto> findJobs(
            String role,
            String location,
            String workMode,
            String technology,
            String source,
            BigDecimal minSalary,
            int page,
            int size,
            UUID userId) {
        return findJobs(role, location, workMode, technology, source, minSalary, null, null, "RECOMMENDED", page, size, userId);
    }

    @Transactional(readOnly = true)
    public Page<JobDto> findJobs(
            String role,
            String location,
            String workMode,
            String technology,
            String source,
            BigDecimal minSalary,
            String priority,
            String freshness,
            String sortBy,
            int page,
            int size,
            UUID userId) {

        if (userId == null) {
            return new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        // Profile-First Gate: Discovery requires a ready profile
        com.jobhunter.dto.profile.ProfileReadinessReport readiness = profileReadinessService.evaluateProfile(profile);
        if (!readiness.isCanDiscover()) {
            return new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
        }

        Specification<Job> spec = (root, query, cb) -> cb.isTrue(root.get("active"));

        if (role != null && !role.isBlank()) {
            String rolePattern = "%" + role.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), rolePattern),
                    cb.like(cb.lower(root.get("normalizedTitle")), rolePattern)
            ));
        }

        if (location != null && !location.isBlank()) {
            String locPattern = "%" + location.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), locPattern));
        }

        if (workMode != null && !workMode.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workMode"), workMode.trim().toUpperCase()));
        }

        if (technology != null && !technology.isBlank()) {
            String techPattern = "%" + technology.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("rawDescriptionMarkdown")), techPattern),
                    cb.like(cb.lower(root.get("structuredJobSpec")), techPattern)
            ));
        }

        if (source != null && !source.isBlank()) {
            String srcPattern = "%" + source.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("jobSource").get("name")), srcPattern));
        }

        if (minSalary != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxSalary"), minSalary));
        }

        List<Job> allMatchingJobs = jobRepository.findAll(spec);

        Map<UUID, Instant> appliedMap = getAppliedJobMap(userId);
        Map<UUID, com.jobhunter.model.entity.JobMatch> matchesMap = getCandidateMatchesMap(userId);

        List<JobDto> usefulJobs = new java.util.ArrayList<>();

        for (Job job : allMatchingJobs) {
            // 1. Hard Eligibility Filter
            com.jobhunter.service.discovery.dto.HardEligibilityResult hardCheck = hardEligibilityFilter.evaluate(job, profile);
            if (!hardCheck.isEligible()) {
                continue;
            }

            // 2. Semantic Match Evaluation
            com.jobhunter.dto.matching.MatchAnalysisResponse matchResp = null;
            if (matchesMap.containsKey(job.getId())) {
                com.jobhunter.model.entity.JobMatch jm = matchesMap.get(job.getId());
                matchResp = semanticMatchingService.buildResponseFromEntity(jm, job);
            } else {
                try {
                    matchResp = semanticMatchingService.analyzeAndMatch(job.getId(), userId, false);
                } catch (Exception ignored) {}
            }

            // 3. Usefulness Gate
            com.jobhunter.dto.matching.UsefulnessDecision decision = usefulnessGate.evaluateUsefulness(job, profile, matchResp);
            if (!decision.isUseful()) {
                continue;
            }

            // 4. Map to DTO with rich attributes
            JobDto dto = discoveryService.mapToDto(job);
            populateFreshness(dto, job.getPostingDate());

            if (appliedMap.containsKey(job.getId())) {
                dto.setApplied(true);
                dto.setAppliedAt(appliedMap.get(job.getId()));
            } else {
                dto.setApplied(false);
                dto.setAppliedAt(null);
            }

            dto.setUsefulnessStatus(decision.getUsefulnessStatus());
            dto.setMatchCategory(decision.getMatchCategory());
            dto.setMatchedRequirements(decision.getMatchedRequirements());
            dto.setMissingRequirements(decision.getMissingRequirements());
            dto.setCandidateEvidence(decision.getCandidateEvidence());
            dto.setHardEligibilityPassed(true);

            if (matchResp != null) {
                dto.setPriorityCategory(matchResp.getPriorityCategory());
                if (matchResp.getPriorityScore() != null) {
                    dto.setPriorityScore(matchResp.getPriorityScore().intValue());
                }
                if (matchResp.getFreshness() != null) {
                    dto.setFreshness(matchResp.getFreshness());
                }
                if (matchResp.getDaysSincePosted() != null) {
                    dto.setDaysSincePosted(matchResp.getDaysSincePosted());
                }
                dto.setWhyThisJob(matchResp.getWhyThisJob());
                dto.setPotentialConcerns(matchResp.getPotentialConcerns());
            }

            usefulJobs.add(dto);
        }

        List<JobDto> filtered = usefulJobs.stream()
                .filter(dto -> {
                    if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority.trim())) {
                        if ("APPLIED".equalsIgnoreCase(priority.trim())) {
                            return dto.isApplied();
                        }
                        if (dto.getPriorityCategory() == null || !dto.getPriorityCategory().equalsIgnoreCase(priority.trim())) {
                            return false;
                        }
                    }
                    if (freshness != null && !freshness.isBlank() && !"ALL".equalsIgnoreCase(freshness.trim())) {
                        if (dto.getFreshness() == null || !dto.getFreshness().equalsIgnoreCase(freshness.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(buildJobComparator(sortBy))
                .toList();

        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<JobDto> pageContent = filtered.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }

    private void populateFreshness(JobDto dto, java.time.LocalDate postingDate) {
        if (postingDate == null) {
            dto.setFreshness("UNKNOWN");
            dto.setDaysSincePosted(null);
            return;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(postingDate, java.time.LocalDate.now());
        int daysSincePosted = (int) Math.max(0, days);
        dto.setDaysSincePosted(daysSincePosted);
        if (daysSincePosted <= 7) {
            dto.setFreshness("NEW");
        } else if (daysSincePosted <= 30) {
            dto.setFreshness("RECENT");
        } else if (daysSincePosted <= 60) {
            dto.setFreshness("OLDER");
        } else {
            dto.setFreshness("STALE");
        }
    }

    private Comparator<JobDto> buildJobComparator(String sortBy) {
        String mode = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toUpperCase() : "RECOMMENDED";
        switch (mode) {
            case "NEWEST" -> {
                return Comparator
                        .comparing(JobDto::getPostingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }
            case "BEST_MATCH" -> {
                return Comparator
                        .comparing((JobDto j) -> j.getPriorityScore() != null ? j.getPriorityScore() : -1, Comparator.reverseOrder())
                        .thenComparing(JobDto::getPostingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }
            case "ALREADY_APPLIED" -> {
                return Comparator
                        .comparing(JobDto::isApplied, Comparator.reverseOrder())
                        .thenComparing((JobDto j) -> j.getPriorityScore() != null ? j.getPriorityScore() : -1, Comparator.reverseOrder())
                        .thenComparing(JobDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }
            case "RECOMMENDED", "MOST_RELEVANT" -> {
                return Comparator
                        .comparing((JobDto j) -> {
                            String cat = j.getPriorityCategory();
                            if ("HIGH_PRIORITY".equalsIgnoreCase(cat)) return 4;
                            if ("MEDIUM_PRIORITY".equalsIgnoreCase(cat)) return 3;
                            if ("LOW_PRIORITY".equalsIgnoreCase(cat)) return 2;
                            if ("NOT_RECOMMENDED".equalsIgnoreCase(cat)) return 1;
                            return 0;
                        }, Comparator.reverseOrder())
                        .thenComparing((JobDto j) -> j.getPriorityScore() != null ? j.getPriorityScore() : -1, Comparator.reverseOrder())
                        .thenComparing(JobDto::getPostingDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }
            default -> {
                return Comparator.comparing(JobDto::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }
        }
    }

    private Map<UUID, com.jobhunter.model.entity.JobMatch> getCandidateMatchesMap(UUID userId) {
        if (userId == null) return Collections.emptyMap();
        return candidateProfileRepository.findByUserId(userId)
                .map(profile -> {
                    List<com.jobhunter.model.entity.JobMatch> matches = jobMatchRepository.findByCandidateProfileId(profile.getId());
                    Map<UUID, com.jobhunter.model.entity.JobMatch> map = new java.util.HashMap<>();
                    for (com.jobhunter.model.entity.JobMatch m : matches) {
                        if (m != null && m.getJob() != null && m.getJob().getId() != null) {
                            map.put(m.getJob().getId(), m);
                        }
                    }
                    return map;
                })
                .orElse(Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public JobDto getJobById(UUID id) {
        return getJobById(id, null);
    }

    @Transactional(readOnly = true)
    public JobDto getJobById(UUID id, UUID userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + id));
        JobDto dto = discoveryService.mapToDto(job);
        populateFreshness(dto, job.getPostingDate());

        if (userId != null) {
            candidateProfileRepository.findByUserId(userId).ifPresent(profile -> {
                applicationRepository.findByCandidateProfileIdAndJobId(profile.getId(), id).ifPresent(app -> {
                    dto.setApplied(app.isApplied());
                    dto.setAppliedAt(app.getAppliedAt());
                });

                jobMatchRepository.findByJobIdAndCandidateProfileId(id, profile.getId()).ifPresent(match -> {
                    dto.setPriorityCategory(match.getPriorityCategory());
                    if (match.getPriorityScore() != null) {
                        dto.setPriorityScore(match.getPriorityScore().intValue());
                    }
                    if (match.getFreshness() != null) {
                        dto.setFreshness(match.getFreshness());
                    }
                    if (match.getDaysSincePosted() != null) {
                        dto.setDaysSincePosted(match.getDaysSincePosted());
                    }
                    try {
                        if (match.getWhyThisJob() != null && !match.getWhyThisJob().isBlank()) {
                            dto.setWhyThisJob(objectMapper.readValue(match.getWhyThisJob(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
                        }
                        if (match.getPotentialConcerns() != null && !match.getPotentialConcerns().isBlank()) {
                            dto.setPotentialConcerns(objectMapper.readValue(match.getPotentialConcerns(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
                        }
                    } catch (Exception ignored) {}
                });
            });
        }
        return dto;
    }

    @Transactional
    public JobAppliedStatusDto setJobAppliedStatus(UUID jobId, UUID userId, boolean applied) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        Optional<Application> existingApp = applicationRepository.findByCandidateProfileIdAndJobId(profile.getId(), jobId);

        Application application;
        Instant now = Instant.now();
        if (existingApp.isPresent()) {
            application = existingApp.get();
            application.setApplied(applied);
            application.setAppliedAt(applied ? (application.getAppliedAt() != null ? application.getAppliedAt() : now) : null);
        } else {
            application = new Application(profile, job, applied);
            application.setAppliedAt(applied ? now : null);
        }

        applicationRepository.save(application);

        return new JobAppliedStatusDto(jobId, application.isApplied(), application.getAppliedAt());
    }

    private Map<UUID, Instant> getAppliedJobMap(UUID userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        return candidateProfileRepository.findByUserId(userId)
                .map(profile -> applicationRepository.findByCandidateProfileIdAndAppliedTrue(profile.getId()).stream()
                        .collect(Collectors.toMap(
                                app -> app.getJob().getId(),
                                app -> app.getAppliedAt() != null ? app.getAppliedAt() : app.getCreatedAt(),
                                (existing, replacement) -> existing
                        )))
                .orElse(Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public List<JobDto> getAppliedJobs(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return Collections.emptyList();
        }
        List<Application> apps = applicationRepository.findByCandidateProfileIdAndAppliedTrue(profile.getId());
        return apps.stream().map(app -> {
            JobDto dto = discoveryService.mapToDto(app.getJob());
            populateFreshness(dto, app.getJob().getPostingDate());
            dto.setApplied(true);
            dto.setAppliedAt(app.getAppliedAt() != null ? app.getAppliedAt() : app.getCreatedAt());
            return dto;
        }).sorted(Comparator.comparing(JobDto::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchRunDto> getRecentSearchRuns() {
        return searchRunRepository.findTop10ByOrderByStartedAtDesc().stream()
                .map(this::mapToSearchRunDto)
                .toList();
    }

    private SearchRunDto mapToSearchRunDto(SearchRun sr) {
        SearchRunDto dto = new SearchRunDto();
        dto.setId(sr.getId());
        if (sr.getJobSource() != null) {
            dto.setJobSourceName(sr.getJobSource().getName());
        }
        dto.setQueryString(sr.getQueryString());
        dto.setStatus(sr.getStatus());
        dto.setJobsDiscoveredCount(sr.getJobsDiscoveredCount());
        dto.setJobsIngestedCount(sr.getJobsIngestedCount());
        dto.setErrorMessage(sr.getErrorMessage());
        dto.setStartedAt(sr.getStartedAt());
        dto.setCompletedAt(sr.getCompletedAt());
        return dto;
    }
}
