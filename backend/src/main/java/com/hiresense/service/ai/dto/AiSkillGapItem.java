package com.hiresense.service.ai.dto;

public record AiSkillGapItem(
        String skill,
        String requiredLevel,
        String currentLevel,
        String gap,
        String explanation
) {}