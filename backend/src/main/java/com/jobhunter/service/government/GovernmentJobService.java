package com.jobhunter.service.government;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.government.*;
import com.jobhunter.model.entity.government.*;
import com.jobhunter.repository.government.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernmentJobService {

    private final GovernmentJobRepository jobRepository;
    private final GovernmentJobEligibilityRepository eligibilityRepository;
    private final GovernmentJobEvidenceRepository evidenceRepository;
    private final GovernmentJobCorrigendumRepository corrigendumRepository;
    private final CandidateGovernmentProfileService candidateProfileService;
    private final GovernmentEligibilityEngine eligibilityEngine;
    private final ObjectMapper objectMapper;

    public GovernmentJobService(
            GovernmentJobRepository jobRepository,
            GovernmentJobEligibilityRepository eligibilityRepository,
            GovernmentJobEvidenceRepository evidenceRepository,
            GovernmentJobCorrigendumRepository corrigendumRepository,
            CandidateGovernmentProfileService candidateProfileService,
            GovernmentEligibilityEngine eligibilityEngine,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.evidenceRepository = evidenceRepository;
        this.corrigendumRepository = corrigendumRepository;
        this.candidateProfileService = candidateProfileService;
        this.eligibilityEngine = eligibilityEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<GovernmentJobDto> searchJobs(GovernmentJobFilterRequest request, UUID currentUserId) {
        Sort sort = Sort.by(Sort.Order.asc("applicationLastDate").nullsLast(), Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<GovernmentJob> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getState() != null && !request.getState().isBlank() && !request.getState().equalsIgnoreCase("ALL")) {
                String stateLower = request.getState().trim().toLowerCase();
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("state")), stateLower),
                        cb.equal(cb.lower(root.get("state")), "all-india"),
                        cb.equal(cb.lower(root.get("state")), "central")
                ));
            }

            if (request.getDistrict() != null && !request.getDistrict().isBlank() && !request.getDistrict().equalsIgnoreCase("ALL")) {
                String distLower = request.getDistrict().trim().toLowerCase();
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("district")), distLower),
                        cb.equal(cb.lower(root.get("district")), "all"),
                        cb.isNull(root.get("district"))
                ));
            }

            if (request.getEmploymentType() != null) {
                predicates.add(cb.equal(root.get("employmentType"), request.getEmploymentType()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getVerificationStatus() != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), request.getVerificationStatus()));
            }

            if (!request.isIncludeUnverified()) {
                predicates.add(cb.notEqual(root.get("verificationStatus"), GovernmentVerificationStatus.UNVERIFIED));
            }

            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String pattern = "%" + request.getQuery().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("organization")), pattern),
                        cb.like(cb.lower(root.get("department")), pattern),
                        cb.like(cb.lower(root.get("authority")), pattern),
                        cb.like(cb.lower(root.get("notificationNumber")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GovernmentJob> entityPage = jobRepository.findAll(spec, pageable);

        CandidateGovernmentProfile profile = null;
        if (currentUserId != null) {
            profile = candidateProfileService.getProfileEntity(currentUserId).orElse(null);
        }

        final CandidateGovernmentProfile finalProfile = profile;
        List<GovernmentJobDto> dtos = entityPage.getContent().stream()
                .map(job -> toDto(job, finalProfile))
                .collect(Collectors.toList());

        // In-memory filter if candidate eligibility filter is set
        if (request.getEligibilityFilter() != null && !request.getEligibilityFilter().equalsIgnoreCase("ALL") && finalProfile != null) {
            if (request.getEligibilityFilter().equalsIgnoreCase("ELIGIBLE_ONLY")) {
                dtos = dtos.stream()
                        .filter(d -> d.getCandidateEligibility() != null && d.getCandidateEligibility().getStatus() == EligibilityStatus.ELIGIBLE)
                        .collect(Collectors.toList());
            } else if (request.getEligibilityFilter().equalsIgnoreCase("LIKELY_ELIGIBLE")) {
                dtos = dtos.stream()
                        .filter(d -> d.getCandidateEligibility() != null &&
                                (d.getCandidateEligibility().getStatus() == EligibilityStatus.ELIGIBLE ||
                                 d.getCandidateEligibility().getStatus() == EligibilityStatus.LIKELY_ELIGIBLE))
                        .collect(Collectors.toList());
            }
        }

        return new PageImpl<>(dtos, pageable, entityPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public GovernmentJobDetailDto getJobDetail(UUID jobId, UUID currentUserId) {
        GovernmentJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Government Job not found: " + jobId));

        CandidateGovernmentProfile profile = null;
        if (currentUserId != null) {
            profile = candidateProfileService.getProfileEntity(currentUserId).orElse(null);
        }

        return toDetailDto(job, profile);
    }

    public GovernmentJobDto toDto(GovernmentJob job, CandidateGovernmentProfile profile) {
        GovernmentJobDto dto = new GovernmentJobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setOrganization(job.getOrganization());
        dto.setDepartment(job.getDepartment());
        dto.setState(job.getState());
        dto.setDistrict(job.getDistrict());
        dto.setBlock(job.getBlock());
        dto.setEmploymentType(job.getEmploymentType());
        dto.setVacanciesCount(job.getVacanciesCount());
        dto.setSalary(job.getSalary());
        dto.setHonorarium(job.isHonorarium());
        dto.setApplicationMode(job.getApplicationMode());
        dto.setApplicationStartDate(job.getApplicationStartDate());
        dto.setApplicationLastDate(job.getApplicationLastDate());
        dto.setSourceUrl(job.getSourceUrl());
        dto.setNotificationUrl(job.getNotificationUrl());
        dto.setApplicationUrl(job.getApplicationUrl());
        dto.setAuthority(job.getAuthority());
        dto.setSourceDomain(job.getSourceDomain());
        dto.setNotificationNumber(job.getNotificationNumber());
        dto.setVerificationStatus(job.getVerificationStatus());
        dto.setAuthenticityScore(job.getAuthenticityScore());
        dto.setAuthenticityLevel(job.getAuthenticityLevel());
        dto.setStatus(job.getStatus());
        dto.setCreatedAt(job.getCreatedAt());

        if (job.getEligibility() != null) {
            GovernmentJobEligibility elig = job.getEligibility();
            dto.setGenderEligibility(elig.getGender().name());
            dto.setMinimumAge(elig.getMinimumAge());
            dto.setMaximumAge(elig.getMaximumAge());
            dto.setEducationList(parseJsonList(elig.getEducationJson()));
            dto.setDomicile(elig.getDomicile());
        }

        if (profile != null) {
            dto.setCandidateEligibility(eligibilityEngine.evaluate(profile, job));
        }

        if (job.getCorrigenda() != null) {
            dto.setCorrigendaCount(job.getCorrigenda().size());
        }

        return dto;
    }

    public GovernmentJobDetailDto toDetailDto(GovernmentJob job, CandidateGovernmentProfile profile) {
        GovernmentJobDetailDto dto = new GovernmentJobDetailDto();
        dto.setId(job.getId());
        dto.setCanonicalId(job.getCanonicalId());
        dto.setTitle(job.getTitle());
        dto.setOrganization(job.getOrganization());
        dto.setDepartment(job.getDepartment());
        dto.setState(job.getState());
        dto.setDistrict(job.getDistrict());
        dto.setBlock(job.getBlock());
        dto.setEmploymentType(job.getEmploymentType());
        dto.setVacanciesCount(job.getVacanciesCount());
        dto.setVacanciesBreakdown(parseJsonList(job.getVacanciesBreakdownJson()));
        dto.setSalary(job.getSalary());
        dto.setSalaryMin(job.getSalaryMin());
        dto.setSalaryMax(job.getSalaryMax());
        dto.setPayLevel(job.getPayLevel());
        dto.setHonorarium(job.isHonorarium());
        dto.setApplicationMode(job.getApplicationMode());
        dto.setApplicationFee(job.getApplicationFee());
        dto.setApplicationStartDate(job.getApplicationStartDate());
        dto.setApplicationLastDate(job.getApplicationLastDate());
        dto.setExamDate(job.getExamDate());
        dto.setInterviewDate(job.getInterviewDate());
        dto.setSourceUrl(job.getSourceUrl());
        dto.setNotificationUrl(job.getNotificationUrl());
        dto.setApplicationUrl(job.getApplicationUrl());
        dto.setAuthority(job.getAuthority());
        dto.setSourceDomain(job.getSourceDomain());
        dto.setNotificationNumber(job.getNotificationNumber());
        dto.setVerificationStatus(job.getVerificationStatus());
        dto.setAuthenticityScore(job.getAuthenticityScore());
        dto.setAuthenticityLevel(job.getAuthenticityLevel());
        dto.setStatus(job.getStatus());
        dto.setRawContent(job.getRawContent());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());

        if (job.getEligibility() != null) {
            GovernmentJobEligibility elig = job.getEligibility();
            dto.setGender(elig.getGender().name());
            dto.setMinimumAge(elig.getMinimumAge());
            dto.setMaximumAge(elig.getMaximumAge());
            dto.setEducation(parseJsonList(elig.getEducationJson()));
            dto.setExperience(parseJsonList(elig.getExperienceJson()));
            dto.setExperienceYearsMin(elig.getExperienceYearsMin());
            dto.setDomicile(elig.getDomicile());
            dto.setCategoryReservations(parseJsonList(elig.getCategoryReservationsJson()));
            dto.setPwdEligible(elig.getPwdEligible());
            dto.setExServicemanEligible(elig.getExServicemanEligible());
            dto.setOtherConditions(elig.getOtherConditions());
        }

        // Field Evidence list (Section 29 & 30)
        List<GovernmentJobEvidence> evidenceEntities = evidenceRepository.findByJobId(job.getId());
        List<GovernmentJobDetailDto.EvidenceItemDto> evDtos = new ArrayList<>();
        for (GovernmentJobEvidence ev : evidenceEntities) {
            evDtos.add(new GovernmentJobDetailDto.EvidenceItemDto(
                    ev.getFieldName(),
                    ev.getFieldValue(),
                    ev.getSourceDocument(),
                    ev.getPageOrSection(),
                    ev.getExcerpt()
            ));
        }
        dto.setEvidenceList(evDtos);

        // Corrigenda list (Section 22)
        List<GovernmentJobCorrigendum> corrigendaEntities = corrigendumRepository.findByJobIdOrderByIssueDateDesc(job.getId());
        List<GovernmentJobDetailDto.CorrigendumItemDto> corrDtos = new ArrayList<>();
        for (GovernmentJobCorrigendum c : corrigendaEntities) {
            corrDtos.add(new GovernmentJobDetailDto.CorrigendumItemDto(
                    c.getNoticeType().name(),
                    c.getTitle(),
                    c.getDocumentUrl(),
                    c.getIssueDate(),
                    c.getDescription(),
                    c.getRevisedLastDate(),
                    c.getRevisedVacancies()
            ));
        }
        dto.setCorrigenda(corrDtos);

        if (profile != null) {
            dto.setCandidateEligibility(eligibilityEngine.evaluate(profile, job));
        }

        return dto;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
