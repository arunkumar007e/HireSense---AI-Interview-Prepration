package com.hiresense.controller;

import com.hiresense.dto.progress.ProgressResponse;
import com.hiresense.dto.progress.ProgressSummaryResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@Tag(name = "Progress", description = "Progress tracking and summaries")
public class ProgressController {

    private final ProgressService progressService;
    private final CurrentUser currentUser;

    public ProgressController(ProgressService progressService, CurrentUser currentUser) {
        this.progressService = progressService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Get all progress entries for the current user")
    public ResponseEntity<List<ProgressResponse>> getProgress() {
        return ResponseEntity.ok(progressService.getUserProgress(currentUser.get().getId()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get an aggregated progress summary")
    public ResponseEntity<ProgressSummaryResponse> getSummary() {
        return ResponseEntity.ok(progressService.getSummary(currentUser.get()));
    }
}