package com.hiresense.dto.learning;

import jakarta.validation.constraints.NotNull;

public record LearningPlanRequest(
        @NotNull(message = "Job role id is required")
        Long jobRoleId,

        Long resumeId,

        Long interviewSessionId
) {}