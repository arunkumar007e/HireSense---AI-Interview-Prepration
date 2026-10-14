package com.hiresense.dto.skill;

import com.hiresense.enums.SkillProficiency;

public record SkillGapItemResponse(
        String skill,
        SkillProficiency requiredLevel,
        SkillProficiency currentLevel,
        String gap,
        String explanation
) {}