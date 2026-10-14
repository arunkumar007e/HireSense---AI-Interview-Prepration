package com.hiresense.service;

import com.hiresense.dto.admin.AdminStatsResponse;
import com.hiresense.dto.auth.UserResponse;
import com.hiresense.entity.AnswerEvaluation;
import com.hiresense.entity.User;
import com.hiresense.enums.InterviewStatus;
import com.hiresense.enums.Role;
import com.hiresense.exception.BadRequestException;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.AnswerEvaluationRepository;
import com.hiresense.repository.InterviewSessionRepository;
import com.hiresense.repository.LearningPlanRepository;
import com.hiresense.repository.ResumeRepository;
import com.hiresense.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository sessionRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final AnswerEvaluationRepository evaluationRepository;

    public AdminService(UserRepository userRepository,
                        ResumeRepository resumeRepository,
                        InterviewSessionRepository sessionRepository,
                        LearningPlanRepository learningPlanRepository,
                        AnswerEvaluationRepository evaluationRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.sessionRepository = sessionRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(Role.ADMIN);
        long candidateUsers = totalUsers - adminUsers;
        long totalResumes = resumeRepository.count();
        long totalInterviews = sessionRepository.count();
        long completedInterviews = sessionRepository.countByStatus(InterviewStatus.COMPLETED);
        long totalLearningPlans = learningPlanRepository.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (InterviewStatus status : InterviewStatus.values()) {
            byStatus.put(status.name(), sessionRepository.countByStatus(status));
        }

        long questionsAnswered = sessionRepository.totalAnsweredQuestionsByAnswer();
        double averageInterviewScore = averageInterviewScore();

        return new AdminStatsResponse(
                totalUsers,
                adminUsers,
                candidateUsers,
                totalResumes,
                totalInterviews,
                completedInterviews,
                totalLearningPlans,
                byStatus,
                questionsAnswered,
                averageInterviewScore);
    }

    @Transactional
    public void changeRole(Long targetUserId, Role newRole, User actor) {
        if (targetUserId.equals(actor.getId())) {
            throw new BadRequestException("You cannot change your own role");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + targetUserId));
        target.setRole(newRole);
        userRepository.save(target);
    }

    private double averageInterviewScore() {
        List<AnswerEvaluation> evaluations = evaluationRepository.findAll();
        if (evaluations.isEmpty()) {
            return 0.0;
        }
        return Math.round(evaluations.stream()
                .mapToInt(e -> e.getScore() == null ? 0 : e.getScore())
                .average()
                .orElse(0) * 10.0) / 10.0;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}