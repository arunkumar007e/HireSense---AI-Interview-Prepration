package com.hiresense.service.ai.dto;

import java.util.List;

public record AiSkillGap(
        int matchScore,
        String summary,
        List<AiSkillGapItem> skills
) {}