package com.hiresense.controller;

import com.hiresense.dto.resume.ResumeAnalysisResponse;
import com.hiresense.dto.resume.ResumeAnalyzeRequest;
import com.hiresense.dto.resume.ResumeSummaryResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@Tag(name = "Resumes", description = "Upload, analyse and manage resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final CurrentUser currentUser;

    public ResumeController(ResumeService resumeService, CurrentUser currentUser) {
        this.resumeService = resumeService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a resume file (PDF, DOCX, TXT...)")
    public ResponseEntity<ResumeSummaryResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.upload(currentUser.get(), file));
    }

    @GetMapping
    @Operation(summary = "List the current user's resumes")
    public ResponseEntity<List<ResumeSummaryResponse>> getMyResumes() {
        return ResponseEntity.ok(resumeService.getUserResumes(currentUser.get().getId()));
    }

    @GetMapping("/{resumeId}")
    @Operation(summary = "Get a single resume")
    public ResponseEntity<ResumeSummaryResponse> getResume(@PathVariable Long resumeId) {
        return ResponseEntity.ok(resumeService.getResume(currentUser.get().getId(), resumeId));
    }

    @PostMapping("/{resumeId}/analyze")
    @Operation(summary = "Analyse a resume against a target job role using AI")
    public ResponseEntity<ResumeAnalysisResponse> analyze(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeAnalyzeRequest request) {
        return ResponseEntity.ok(resumeService.analyze(currentUser.get(), resumeId, request.jobRoleId()));
    }

    @DeleteMapping("/{resumeId}")
    @Operation(summary = "Delete a resume")
    public ResponseEntity<Void> delete(@PathVariable Long resumeId) {
        resumeService.delete(currentUser.get(), resumeId);
        return ResponseEntity.noContent().build();
    }
}