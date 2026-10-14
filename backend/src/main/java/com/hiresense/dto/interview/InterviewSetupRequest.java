package com.hiresense.dto.interview;

import com.hiresense.enums.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InterviewSetupRequest(
        @NotNull(message = "Job role id is required")
        Long jobRoleId,

        @Min(value = 3, message = "At least 3 questions required")
        @Max(value = 20, message = "At most 20 questions allowed")
        Integer numberOfQuestions,

        DifficultyLevel difficulty,

        Integer timeLimitSeconds
) {}