package com.hiresense.controller;

import com.hiresense.dto.skill.JobRoleResponse;
import com.hiresense.dto.skill.SkillGapRequest;
import com.hiresense.dto.skill.SkillGapResponse;
import com.hiresense.dto.skill.SkillResponse;
import com.hiresense.security.CurrentUser;
import com.hiresense.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Job Roles & Skills", description = "Job role catalogue and skill gap analysis")
public class JobRoleController {

    private final SkillService skillService;
    private final CurrentUser currentUser;

    public JobRoleController(SkillService skillService, CurrentUser currentUser) {
        this.skillService = skillService;
        this.currentUser = currentUser;
    }

    @GetMapping("/job-roles")
    @Operation(summary = "List all job roles with their required skills")
    public ResponseEntity<List<JobRoleResponse>> getJobRoles() {
        return ResponseEntity.ok(skillService.getAllJobRoles());
    }

    @GetMapping("/job-roles/{id}")
    @Operation(summary = "Get a single job role")
    public ResponseEntity<JobRoleResponse> getJobRole(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getJobRole(id));
    }

    @GetMapping("/skills")
    @Operation(summary = "List all skills in the catalogue")
    public ResponseEntity<List<SkillResponse>> getSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @PostMapping("/job-roles/{id}/skill-gap")
    @Operation(summary = "Analyse the skill gap between a candidate and a job role")
    public ResponseEntity<SkillGapResponse> analyzeSkillGap(
            @PathVariable Long id,
            @Valid @RequestBody SkillGapRequest request) {
        return ResponseEntity.ok(skillService.analyzeSkillGap(id, request.resumeSkills()));
    }
}