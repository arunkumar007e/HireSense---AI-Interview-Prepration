package com.hiresense.service.ai.dto;

import java.util.List;

public record AiAnswerEvaluation(
        int score,
        int relevanceScore,
        int clarityScore,
        int depthScore,
        String feedback,
        String topic,
        List<String> strengths,
        List<String> improvements,
        List<String> missingKeywords,
        String suggestedAnswer
) {}