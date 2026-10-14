package com.hiresense.dto.interview;

import com.hiresense.enums.DifficultyLevel;
import com.hiresense.enums.QuestionCategory;

public record InterviewQuestionResponse(
        Long id,
        Long sessionId,
        String content,
        QuestionCategory category,
        DifficultyLevel difficulty,
        int orderIndex,
        boolean answered
) {}