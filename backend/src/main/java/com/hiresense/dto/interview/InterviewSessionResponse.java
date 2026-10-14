package com.hiresense.dto.interview;

import com.hiresense.enums.DifficultyLevel;
import com.hiresense.enums.InterviewStatus;

import java.time.Instant;

public record InterviewSessionResponse(
        Long id,
        Long jobRoleId,
        String jobRoleTitle,
        InterviewStatus status,
        DifficultyLevel difficulty,
        int numberOfQuestions,
        Integer timeLimitSeconds,
        int currentQuestionIndex,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {}