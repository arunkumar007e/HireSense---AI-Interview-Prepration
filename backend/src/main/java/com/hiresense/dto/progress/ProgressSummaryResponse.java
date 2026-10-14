package com.hiresense.dto.progress;

import java.util.List;

public record ProgressSummaryResponse(
        int interviewsCompleted,
        int interviewScoreAverage,
        int questionsAnswered,
        int learningPlansCreated,
        int learningPlanCompletionPercent,
        List<ProgressResponse> recentProgress
) {}