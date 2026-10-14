package com.hiresense.dto.interview;

public record QuestionResultResponse(
        Long questionId,
        String question,
        String category,
        String userAnswer,
        int score,
        String feedback,
        java.util.List<String> strengths,
        java.util.List<String> improvements,
        java.util.List<String> missingKeywords,
        String suggestedAnswer
) {}