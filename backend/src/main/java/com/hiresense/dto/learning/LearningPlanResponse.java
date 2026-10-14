package com.hiresense.dto.learning;

import java.util.List;

public record LearningPlanResponse(
        Long id,
        String title,
        String description,
        List<String> focusSkills,
        String status,
        int totalItems,
        int completedItems,
        int progressPercent,
        List<LearningPlanItemResponse> items,
        java.time.Instant createdAt
) {}