package com.hiresense.dto.skill;

import java.util.List;

public record JobRoleResponse(
        Long id,
        String title,
        String description,
        String category,
        List<SkillResponse> requiredSkills
) {}