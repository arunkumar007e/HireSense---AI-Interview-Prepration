package com.hiresense.service;

import com.hiresense.dto.resume.ResumeAnalysisResponse;
import com.hiresense.dto.resume.ResumeSummaryResponse;
import com.hiresense.entity.JobRole;
import com.hiresense.entity.Resume;
import com.hiresense.entity.Skill;
import com.hiresense.entity.User;
import com.hiresense.enums.ProgressMetric;
import com.hiresense.exception.BadRequestException;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.JobRoleRepository;
import com.hiresense.repository.ProgressRepository;
import com.hiresense.repository.ResumeRepository;
import com.hiresense.service.ai.AiService;
import com.hiresense.service.ai.dto.AiResumeAnalysis;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "doc", "txt", "md", "rtf", "odt");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final ResumeRepository resumeRepository;
    private final JobRoleRepository jobRoleRepository;
    private final SkillService skillService;
    private final AiService aiService;
    private final ProgressService progressService;
    private final Tika tika = new Tika();
    private final Path uploadDir;

    public ResumeService(ResumeRepository resumeRepository,
                         JobRoleRepository jobRoleRepository,
                         SkillService skillService,
                         AiService aiService,
                         ProgressService progressService,
                         @Value("${app.upload-dir}") String uploadDir) {
        this.resumeRepository = resumeRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.skillService = skillService;
        this.aiService = aiService;
        this.progressService = progressService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        createUploadDir();
    }

    private void createUploadDir() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            log.error("Could not create upload directory {}", uploadDir, ex);
        }
    }

    @Transactional
    public ResumeSummaryResponse upload(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds the 10MB limit");
        }
        String originalName = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type. Please upload PDF, DOCX, DOC, TXT, MD, RTF or ODT");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(storedName);

        String extractedText;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            extractedText = tika.parseToString(target.toFile());
        } catch (Exception ex) {
            throw new BadRequestException("Could not read the uploaded file: " + ex.getMessage());
        }
        if (extractedText == null || extractedText.isBlank()) {
            throw new BadRequestException("No readable text could be extracted from the file");
        }

        List<String> skills = aiService.extractSkills(extractedText);
        Set<Skill> parsedSkills = skillService.ensureSkills(skills);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(originalName);
        resume.setFileType(extension);
        resume.setFileSize(file.getSize());
        resume.setStorageUrl(target.toString());
        resume.setExtractedText(extractedText);
        resume.setParsedSkills(parsedSkills);
        resume.setAnalysisStatus("READY");
        resume = resumeRepository.save(resume);

        if (!skills.isEmpty()) {
            progressService.record(user, ProgressMetric.RESUME_ANALYSIS, 100,
                    "Skills extracted", originalName + " - " + skills.size() + " skills found");
        }
        return toSummaryResponse(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeSummaryResponse> getUserResumes(Long userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeSummaryResponse getResume(Long userId, Long resumeId) {
        Resume resume = findOwned(userId, resumeId);
        return toSummaryResponse(resume);
    }

    @Transactional
    public ResumeAnalysisResponse analyze(User user, Long resumeId, Long jobRoleId) {
        Resume resume = findOwned(user.getId(), resumeId);
        JobRole role = jobRoleRepository.findById(jobRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Job role not found with id " + jobRoleId));

        List<String> targetSkills = role.getRequiredSkills().stream().map(Skill::getName).toList();
        AiResumeAnalysis analysis = aiService.analyzeResume(resume.getExtractedText(), targetSkills);

        resume.setAnalysisSummary(analysis.summary());
        resume.setAtsScore(clamp(analysis.atsScore()));
        resume.setKeywordMatchScore(clamp(analysis.keywordMatchScore()));
        resume.setParsedSkills(skillService.ensureSkills(analysis.extractedSkills()));
        resume.setAnalysisStatus("ANALYZED");
        resume.setAnalyzedAt(Instant.now());
        resume = resumeRepository.save(resume);

        progressService.record(user, ProgressMetric.RESUME_ANALYSIS,
                clamp(analysis.atsScore()), "Resume analyzed",
                fileName(resume.getFileName()) + " vs " + role.getTitle());

        return new ResumeAnalysisResponse(
                resume.getId(),
                resume.getFileName(),
                clamp(analysis.atsScore()),
                clamp(analysis.keywordMatchScore()),
                analysis.extractedSkills(),
                analysis.summary(),
                analysis.strengths(),
                analysis.improvements(),
                resume.getAnalysisStatus());
    }

    @Transactional
    public void delete(User user, Long resumeId) {
        Resume resume = findOwned(user.getId(), resumeId);
        if (resume.getStorageUrl() != null) {
            try {
                Files.deleteIfExists(Paths.get(resume.getStorageUrl()));
            } catch (IOException ex) {
                log.warn("Could not delete resume file {}", resume.getStorageUrl(), ex);
            }
        }
        resumeRepository.delete(resume);
    }

    private Resume findOwned(Long userId, Long resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id " + resumeId));
    }

    private ResumeSummaryResponse toSummaryResponse(Resume resume) {
        return new ResumeSummaryResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getFileType(),
                resume.getFileSize(),
                resume.getAnalysisStatus(),
                resume.getAtsScore(),
                resume.getKeywordMatchScore(),
                resume.getAnalysisSummary(),
                resume.getParsedSkills().stream().map(Skill::getName).toList(),
                resume.getCreatedAt(),
                resume.getAnalyzedAt());
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String fileName(String name) {
        return name == null ? "resume" : name;
    }
}