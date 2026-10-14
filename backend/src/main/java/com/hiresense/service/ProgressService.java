package com.hiresense.service;

import com.hiresense.dto.progress.ProgressResponse;
import com.hiresense.dto.progress.ProgressSummaryResponse;
import com.hiresense.entity.LearningPlan;
import com.hiresense.entity.Progress;
import com.hiresense.entity.User;
import com.hiresense.enums.ProgressMetric;
import com.hiresense.repository.LearningPlanRepository;
import com.hiresense.repository.ProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final LearningPlanRepository learningPlanRepository;

    public ProgressService(ProgressRepository progressRepository, LearningPlanRepository learningPlanRepository) {
        this.progressRepository = progressRepository;
        this.learningPlanRepository = learningPlanRepository;
    }

    @Transactional
    public void record(User user, ProgressMetric metric, int value, String title, String note) {
        Progress progress = new Progress();
        progress.setUser(user);
        progress.setMetricType(metric);
        progress.setMetricValue(clamp(value));
        progress.setRelatedTitle(title);
        progress.setNote(note);
        progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getUserProgress(Long userId) {
        return progressRepository.findByUserIdOrderByRecordedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse getSummary(User user) {
        List<Progress> interviews = progressRepository.findByUserIdAndMetricTypeOrderByRecordedAtDesc(
                user.getId(), ProgressMetric.INTERVIEW_SCORE);
        List<Progress> answers = progressRepository.findByUserIdAndMetricTypeOrderByRecordedAtDesc(
                user.getId(), ProgressMetric.QUESTIONS_ANSWERED);

        int interviewsCompleted = interviews.size();
        int interviewScoreAverage = interviews.isEmpty()
                ? 0
                : (int) Math.round(interviews.stream().mapToInt(Progress::getMetricValue).average().orElse(0));
        int questionsAnswered = answers.size();

        List<LearningPlan> plans = learningPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        int plansCreated = plans.size();
        int planCompletionPercent = planCompletionPercent(plans);

        List<ProgressResponse> recent = progressRepository.findByUserIdOrderByRecordedAtDesc(user.getId())
                .stream()
                .limit(10)
                .map(this::toResponse)
                .toList();

        return new ProgressSummaryResponse(
                interviewsCompleted,
                interviewScoreAverage,
                questionsAnswered,
                plansCreated,
                planCompletionPercent,
                recent);
    }

    private int planCompletionPercent(List<LearningPlan> plans) {
        int totalItems = 0;
        int completedItems = 0;
        for (LearningPlan plan : plans) {
            totalItems += plan.getItems().size();
            completedItems += (int) plan.getItems().stream().filter(i -> i.isCompleted()).count();
        }
        if (totalItems == 0) {
            return 0;
        }
        return (int) Math.round(completedItems * 100.0 / totalItems);
    }

    private ProgressResponse toResponse(Progress progress) {
        return new ProgressResponse(
                progress.getId(),
                progress.getMetricType(),
                progress.getMetricValue(),
                progress.getRelatedTitle(),
                progress.getNote(),
                progress.getRecordedAt());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}