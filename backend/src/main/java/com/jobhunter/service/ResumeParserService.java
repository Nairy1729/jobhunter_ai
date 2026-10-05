package com.jobhunter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunter.model.dto.ResumeUploadResponse;
import com.jobhunter.model.entity.CandidateProfile;
import com.jobhunter.model.entity.Resume;
import com.jobhunter.model.entity.Skill;
import com.jobhunter.repository.CandidateProfileRepository;
import com.jobhunter.repository.ResumeRepository;
import com.jobhunter.repository.SkillRepository;
import com.jobhunter.service.ai.ResumeAiOrchestratorService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ResumeAiOrchestratorService resumeAiOrchestratorService;
    private final ObjectMapper objectMapper;
    private final String uploadDir;

    public ResumeParserService(ResumeRepository resumeRepository,
                               CandidateProfileRepository profileRepository,
                               SkillRepository skillRepository,
                               ResumeAiOrchestratorService resumeAiOrchestratorService,
                               ObjectMapper objectMapper,
                               @Value("${storage.upload-dir}") String uploadDir) {
        this.resumeRepository = resumeRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.resumeAiOrchestratorService = resumeAiOrchestratorService;
        this.objectMapper = objectMapper;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public ResumeUploadResponse uploadAndParseMasterResume(UUID userId, MultipartFile file) throws IOException {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate profile not found for user: " + userId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // 1. Enforce strict maximum file size (10 MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        // 2. Sanitize filename to prevent Path Traversal
        String rawFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String cleanBaseName = Paths.get(rawFilename).getFileName().toString()
                .replaceAll("[\\r\\n\\00]", "")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleanBaseName.isBlank() || cleanBaseName.equals(".")) {
            cleanBaseName = "resume.pdf";
        }
        String originalFilename = cleanBaseName;

        // 3. Strict extension allowlist
        String extension = getFileExtension(cleanBaseName).toLowerCase();
        Set<String> allowedExtensions = Set.of("pdf", "txt", "docx");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type [." + extension + "]. Only PDF, TXT, and DOCX files are permitted.");
        }

        byte[] fileBytes = file.getBytes();

        // 4. Verify PDF magic bytes header (%PDF-)
        if ("pdf".equalsIgnoreCase(extension)) {
            if (fileBytes.length < 4 || fileBytes[0] != 0x25 || fileBytes[1] != 0x50 || fileBytes[2] != 0x44 || fileBytes[3] != 0x46) {
                throw new IllegalArgumentException("Invalid file format: Corrupted or forged PDF header.");
            }
        }
        
        // Ensure storage directory exists
        Path storagePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        String savedFileName = UUID.randomUUID() + "_" + cleanBaseName;
        Path targetPath = storagePath.resolve(savedFileName).normalize();

        // 5. Verify target path is strictly contained within storage directory
        if (!targetPath.startsWith(storagePath)) {
            throw new SecurityException("Path traversal attempt detected in upload filename: " + rawFilename);
        }

        Files.write(targetPath, fileBytes);

        // Extract text
        String extractedText;
        if ("pdf".equalsIgnoreCase(extension)) {
            extractedText = extractTextFromPdf(fileBytes);
        } else {
            extractedText = new String(fileBytes, StandardCharsets.UTF_8);
        }

        // Run AI Orchestration Layer to extract and auto-fill profile, skills, experiences, and projects!
        ResumeAiOrchestratorService.ExtractionResult aiResult = resumeAiOrchestratorService.orchestrateAndAutoFill(
                profile, extractedText, originalFilename
        );

        // Detect skills in text
        List<String> detectedSkills = aiResult != null && !aiResult.skills.isEmpty()
                ? aiResult.skills.stream().map(s -> s.name).distinct().toList()
                : detectSkillsInText(extractedText);

        // Build structured metadata
        Map<String, Object> structuredMeta = new HashMap<>();
        structuredMeta.put("originalFilename", originalFilename);
        structuredMeta.put("characterCount", extractedText.length());
        structuredMeta.put("detectedSkills", detectedSkills);
        structuredMeta.put("extractionSource", aiResult != null ? aiResult.extractionSource : "DETERMINISTIC_NLP");
        structuredMeta.put("uploadedAt", new Date().toString());

        Resume resume = new Resume();
        resume.setCandidateProfile(profile);
        resume.setTitle(originalFilename);
        resume.setFilePath(targetPath.toAbsolutePath().toString());
        resume.setFileType(extension.toUpperCase());
        resume.setFileSizeBytes(file.getSize());
        resume.setRawExtractedText(extractedText);
        resume.setStructuredContent(objectMapper.writeValueAsString(structuredMeta));
        resume.setMaster(true);

        Resume savedResume = resumeRepository.save(resume);

        int expCount = aiResult != null ? aiResult.experiences.size() : 0;
        int projCount = aiResult != null ? aiResult.projects.size() : 0;
        String message = String.format(
                "Master resume ingested & profile auto-filled via %s: %d skills, %d experiences, %d projects extracted.",
                aiResult != null ? aiResult.extractionSource : "AI Orchestrator",
                detectedSkills.size(), expCount, projCount
        );

        return new ResumeUploadResponse(
                savedResume.getId(),
                savedResume.getTitle(),
                savedResume.getFileType(),
                savedResume.getFileSizeBytes(),
                extractedText.length(),
                detectedSkills,
                message,
                aiResult != null ? aiResult.headline : profile.getHeadline(),
                aiResult != null ? aiResult.yearsOfExperience : profile.getYearsOfExperience(),
                expCount,
                projCount,
                aiResult != null ? aiResult.extractionSource : "AI_ORCHESTRATOR"
        );
    }

    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (Exception e) {
            log.error("Failed to parse PDF text", e);
            throw new IOException("Failed to extract text from PDF file", e);
        }
    }

    private List<String> detectSkillsInText(String text) {
        String lowerText = text.toLowerCase();
        List<Skill> allSkills = skillRepository.findAll();
        List<String> matches = new ArrayList<>();

        for (Skill s : allSkills) {
            String skillLower = s.getName().toLowerCase();
            if (lowerText.contains(skillLower)) {
                matches.add(s.getName());
            }
        }
        return matches;
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
