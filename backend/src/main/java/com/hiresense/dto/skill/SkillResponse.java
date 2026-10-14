package com.hiresense.dto.skill;

public record SkillResponse(
        Long id,
        String name,
        String category,
        String description
) {}