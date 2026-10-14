package com.hiresense.dto.interview;

public record AnswerEvaluationResponse(
        Long answerId,
        Long questionId,
        String question,
        int score,
        int relevanceScore,
        int clarityScore,
        int depthScore,
        String feedback,
        String topic,
        java.util.List<String> strengths,
        java.util.List<String> improvements,
        java.util.List<String> missingKeywords,
        String suggestedAnswer
) {}