package com.hiresense.service.ai.dto;

public record AiLearningPlanItem(
        String title,
        String description,
        String resourceUrl,
        String skillName,
        int orderIndex,
        int targetDays
) {}