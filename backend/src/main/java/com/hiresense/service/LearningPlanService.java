package com.hiresense.service;

import com.hiresense.dto.learning.LearningPlanItemResponse;
import com.hiresense.dto.learning.LearningPlanRequest;
import com.hiresense.dto.learning.LearningPlanResponse;
import com.hiresense.entity.AnswerEvaluation;
import com.hiresense.entity.InterviewSession;
import com.hiresense.entity.JobRole;
import com.hiresense.entity.LearningPlan;
import com.hiresense.entity.LearningPlanItem;
import com.hiresense.entity.Resume;
import com.hiresense.entity.User;
import com.hiresense.enums.LearningPlanStatus;
import com.hiresense.enums.ProgressMetric;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.AnswerEvaluationRepository;
import com.hiresense.repository.InterviewSessionRepository;
import com.hiresense.repository.JobRoleRepository;
import com.hiresense.repository.LearningPlanRepository;
import com.hiresense.repository.ResumeRepository;
import com.hiresense.service.ai.AiService;
import com.hiresense.service.ai.dto.AiLearningPlan;
import com.hiresense.service.ai.dto.AiLearningPlanItem;
import com.hiresense.service.ai.dto.AiSkillGap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LearningPlanService {

    private final LearningPlanRepository learningPlanRepository;
    private final JobRoleRepository jobRoleRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final ResumeRepository resumeRepository;
    private final AiService aiService;
    private final ProgressService progressService;

    public LearningPlanService(LearningPlanRepository learningPlanRepository,
                               JobRoleRepository jobRoleRepository,
                               InterviewSessionRepository sessionRepository,
                               AnswerEvaluationRepository evaluationRepository,
                               ResumeRepository resumeRepository,
                               AiService aiService,
                               ProgressService progressService) {
        this.learningPlanRepository = learningPlanRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.resumeRepository = resumeRepository;
        this.aiService = aiService;
        this.progressService = progressService;
    }

    @Transactional
    public LearningPlanResponse generate(User user, LearningPlanRequest request) {
        JobRole role = jobRoleRepository.findById(request.jobRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Job role not found with id " + request.jobRoleId()));

        WeakSkillsContext context = collectWeakSkills(user, role, request);

        AiLearningPlan aiPlan = aiService.generateLearningPlan(
                role.getTitle(), context.weakSkills(), context.summary());

        LearningPlan plan = new LearningPlan();
        plan.setUser(user);
        plan.setTitle(aiPlan.title());
        plan.setDescription(aiPlan.description());
        plan.setFocusSkills(String.join(",", context.weakSkills()));
        plan.setStatus(LearningPlanStatus.ACTIVE);

        List<LearningPlanItem> items = new ArrayList<>();
        int order = 1;
        for (AiLearningPlanItem source : aiPlan.items()) {
            if (source.title() == null || source.title().isBlank()) {
                continue;
            }
            LearningPlanItem item = new LearningPlanItem();
            item.setPlan(plan);
            item.setTitle(source.title());
            item.setDescription(source.description());
            item.setResourceUrl(source.resourceUrl());
            item.setSkillName(source.skillName());
            item.setOrderIndex(source.orderIndex() > 0 ? source.orderIndex() : order);
            item.setTargetDays(source.targetDays() > 0 ? source.targetDays() : 5);
            items.add(item);
            order++;
        }
        if (items.isEmpty()) {
            throw new com.hiresense.exception.BadRequestException("Could not generate a learning plan");
        }
        plan.setItems(items);
        plan = learningPlanRepository.save(plan);

        if (!context.weakSkills().isEmpty()) {
            progressService.record(user, ProgressMetric.PLAN_COMPLETION, 0,
                    "Learning plan generated", role.getTitle());
        }
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<LearningPlanResponse> getUserPlans(User user) {
        return learningPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningPlanResponse getPlan(User user, Long planId) {
        return toResponse(findOwned(user, planId));
    }

    @Transactional
    public LearningPlanResponse completeItem(User user, Long planId, Long itemId) {
        LearningPlan plan = findOwned(user, planId);
        LearningPlanItem item = plan.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan item not found with id " + itemId));
        if (!item.isCompleted()) {
            item.setCompleted(true);
            item.setCompletedAt(Instant.now());
        }
        boolean allDone = plan.getItems().stream().allMatch(LearningPlanItem::isCompleted);
        if (allDone) {
            plan.setStatus(LearningPlanStatus.COMPLETED);
        }
        plan = learningPlanRepository.save(plan);

        int percent = (int) Math.round(plan.getItems().stream().filter(LearningPlanItem::isCompleted).count()
                * 100.0 / plan.getItems().size());
        progressService.record(user, ProgressMetric.PLAN_COMPLETION, percent,
                plan.getTitle(), item.getTitle());
        return toResponse(plan);
    }

    @Transactional
    public void delete(User user, Long planId) {
        LearningPlan plan = findOwned(user, planId);
        learningPlanRepository.delete(plan);
    }

    // ------------------------------------------------------------------

    private WeakSkillsContext collectWeakSkills(User user, JobRole role, LearningPlanRequest request) {
        Set<String> weak = new LinkedHashSet<>();
        String summary = "";

        if (request.interviewSessionId() != null) {
            InterviewSession session = sessionRepository.findByIdAndUserId(
                            request.interviewSessionId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Interview session not found"));
            List<AnswerEvaluation> evaluations = evaluationRepository.findBySession(session);
            for (AnswerEvaluation e : evaluations) {
                if (e.getScore() != null && e.getScore() < 70
                        && e.getTopic() != null && !e.getTopic().isBlank()) {
                    weak.add(e.getTopic());
                }
            }
        }

        if (request.resumeId() != null) {
            Resume resume = resumeRepository.findByIdAndUserId(request.resumeId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
            List<String> resumeSkills = resume.getParsedSkills().stream()
                    .map(s -> s.getName())
                    .toList();
            List<String> required = role.getRequiredSkills().stream().map(s -> s.getName()).toList();
            AiSkillGap gap = aiService.analyzeSkillGap(resumeSkills, required, role.getTitle());
            summary = gap.summary();
            if (gap.skills() != null) {
                for (var item : gap.skills()) {
                    if (item.gap() != null && !"none".equalsIgnoreCase(item.gap())) {
                        weak.add(item.skill());
                    }
                }
            }
        }

        if (weak.isEmpty()) {
            role.getRequiredSkills().stream().map(s -> s.getName()).forEach(weak::add);
        }
        return new WeakSkillsContext(new ArrayList<>(weak), summary);
    }

    private LearningPlan findOwned(User user, Long planId) {
        return learningPlanRepository.findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning plan not found with id " + planId));
    }

    private LearningPlanResponse toResponse(LearningPlan plan) {
        int total = plan.getItems().size();
        int completed = (int) plan.getItems().stream().filter(LearningPlanItem::isCompleted).count();
        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);

        List<LearningPlanItemResponse> items = plan.getItems().stream()
                .map(item -> new LearningPlanItemResponse(
                        item.getId(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getResourceUrl(),
                        item.getSkillName(),
                        item.getOrderIndex(),
                        item.getTargetDays(),
                        item.isCompleted(),
                        item.getCompletedAt()))
                .toList();

        List<String> focus = new ArrayList<>();
        if (plan.getFocusSkills() != null) {
            for (String s : plan.getFocusSkills().split(",")) {
                if (!s.isBlank()) {
                    focus.add(s.trim());
                }
            }
        }

        return new LearningPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getDescription(),
                focus,
                plan.getStatus().name(),
                total,
                completed,
                percent,
                items,
                plan.getCreatedAt());
    }

    private record WeakSkillsContext(List<String> weakSkills, String summary) {
    }
}