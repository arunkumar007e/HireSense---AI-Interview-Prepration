package com.hiresense.controller;

import com.hiresense.dto.learning.LearningPlanRequest;
import com.hiresense.dto.learning.LearningPlanResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.LearningPlanService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning-plans")
@Tag(name = "Learning Plans", description = "Personalized AI learning plans and item completion")
public class LearningPlanController {

    private final LearningPlanService learningPlanService;
    private final CurrentUser currentUser;

    public LearningPlanController(LearningPlanService learningPlanService, CurrentUser currentUser) {
        this.learningPlanService = learningPlanService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Generate a personalized learning plan via AI")
    public ResponseEntity<LearningPlanResponse> generate(@Valid @RequestBody LearningPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(learningPlanService.generate(currentUser.get(), request));
    }

    @GetMapping
    @Operation(summary = "List the current user's learning plans")
    public ResponseEntity<List<LearningPlanResponse>> getMyPlans() {
        return ResponseEntity.ok(learningPlanService.getUserPlans(currentUser.get()));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a single learning plan")
    public ResponseEntity<LearningPlanResponse> getPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(learningPlanService.getPlan(currentUser.get(), planId));
    }

    @PostMapping("/{planId}/items/{itemId}/complete")
    @Operation(summary = "Mark a learning plan item as complete")
    public ResponseEntity<LearningPlanResponse> completeItem(
            @PathVariable Long planId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(learningPlanService.completeItem(currentUser.get(), planId, itemId));
    }

    @DeleteMapping("/{planId}")
    @Operation(summary = "Delete a learning plan")
    public ResponseEntity<Void> delete(@PathVariable Long planId) {
        learningPlanService.delete(currentUser.get(), planId);
        return ResponseEntity.noContent().build();
    }
}