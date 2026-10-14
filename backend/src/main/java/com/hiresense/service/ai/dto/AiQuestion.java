package com.hiresense.service.ai.dto;

import java.util.List;

public record AiQuestion(
        String question,
        String category,
        String difficulty,
        List<String> expectedKeywords
) {}