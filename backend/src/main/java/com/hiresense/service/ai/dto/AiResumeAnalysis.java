package com.hiresense.service.ai.dto;

import java.util.List;

public record AiResumeAnalysis(
        int atsScore,
        int keywordMatchScore,
        List<String> extractedSkills,
        String summary,
        List<String> strengths,
        List<String> improvements
) {}