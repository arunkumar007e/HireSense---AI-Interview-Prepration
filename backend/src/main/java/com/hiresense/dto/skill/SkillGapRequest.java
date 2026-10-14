package com.hiresense.dto.skill;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SkillGapRequest(
        @NotNull(message = "Resume skills are required")
        List<String> resumeSkills
) {}