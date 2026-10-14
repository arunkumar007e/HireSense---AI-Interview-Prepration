package com.hiresense.dto.admin;

import java.util.List;
import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        long adminUsers,
        long candidateUsers,
        long totalResumes,
        long totalInterviews,
        long completedInterviews,
        long totalLearningPlans,
        Map<String, Long> interviewsByStatus,
        long questionsAnswered,
        double averageInterviewScore
) {}