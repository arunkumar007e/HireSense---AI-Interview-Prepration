package com.hiresense.dto.interview;

import java.time.Instant;
import java.util.List;

public record InterviewResultResponse(
        Long sessionId,
        String jobRoleTitle,
        int overallScore,
        String summary,
        List<QuestionResultResponse> questions
) {}