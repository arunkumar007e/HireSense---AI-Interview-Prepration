package com.hiresense.service.ai.dto;

import java.util.List;

public record AiLearningPlan(
        String title,
        String description,
        List<AiLearningPlanItem> items
) {}