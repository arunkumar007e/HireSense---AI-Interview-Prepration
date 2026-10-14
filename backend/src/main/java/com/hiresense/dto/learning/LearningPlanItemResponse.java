package com.hiresense.dto.learning;

public record LearningPlanItemResponse(
        Long id,
        String title,
        String description,
        String resourceUrl,
        String skillName,
        int orderIndex,
        Integer targetDays,
        boolean completed,
        java.time.Instant completedAt
) {}