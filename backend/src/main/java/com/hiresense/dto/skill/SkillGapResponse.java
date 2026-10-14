package com.hiresense.dto.skill;

import java.util.List;

public record SkillGapResponse(
        Long jobRoleId,
        String jobRoleTitle,
        int matchScore,
        String summary,
        List<SkillGapItemResponse> skills
) {}