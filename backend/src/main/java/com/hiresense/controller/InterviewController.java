package com.hiresense.controller;

import com.hiresense.dto.interview.AnswerEvaluationResponse;
import com.hiresense.dto.interview.AnswerSubmitRequest;
import com.hiresense.dto.interview.InterviewQuestionResponse;
import com.hiresense.dto.interview.InterviewResultResponse;
import com.hiresense.dto.interview.InterviewSessionResponse;
import com.hiresense.dto.interview.InterviewSetupRequest;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@Tag(name = "Interviews", description = "Mock interview setup, execution and results")
public class InterviewController {

    private final InterviewService interviewService;
    private final CurrentUser currentUser;

    public InterviewController(InterviewService interviewService, CurrentUser currentUser) {
        this.interviewService = interviewService;
        this.currentUser = currentUser;
    }

    @PostMapping("/setup")
    @Operation(summary = "Set up a new mock interview for a job role")
    public ResponseEntity<InterviewSessionResponse> setup(@Valid @RequestBody InterviewSetupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.setup(currentUser.get(), request));
    }

    @GetMapping
    @Operation(summary = "List the current user's interview sessions")
    public ResponseEntity<List<InterviewSessionResponse>> getMySessions() {
        return ResponseEntity.ok(interviewService.getUserSessions(currentUser.get()));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a single interview session")
    public ResponseEntity<InterviewSessionResponse> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(interviewService.getSession(currentUser.get(), sessionId));
    }

    @PostMapping("/{sessionId}/start")
    @Operation(summary = "Mark an interview as in progress")
    public ResponseEntity<InterviewSessionResponse> start(@PathVariable Long sessionId) {
        return ResponseEntity.ok(interviewService.start(currentUser.get(), sessionId));
    }

    @GetMapping("/{sessionId}/questions")
    @Operation(summary = "Get all questions for an interview session")
    public ResponseEntity<List<InterviewQuestionResponse>> getQuestions(@PathVariable Long sessionId) {
        return ResponseEntity.ok(interviewService.getQuestions(currentUser.get(), sessionId));
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    @Operation(summary = "Submit an answer to a question and receive an AI evaluation")
    public ResponseEntity<AnswerEvaluationResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.submitAnswer(currentUser.get(), sessionId, questionId, request));
    }

    @GetMapping("/{sessionId}/result")
    @Operation(summary = "Get the interview result with per-question evaluations")
    public ResponseEntity<InterviewResultResponse> getResult(@PathVariable Long sessionId) {
        return ResponseEntity.ok(interviewService.getResult(currentUser.get(), sessionId));
    }
}