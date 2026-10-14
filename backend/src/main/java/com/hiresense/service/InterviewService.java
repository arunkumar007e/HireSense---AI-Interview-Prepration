package com.hiresense.service;

import com.hiresense.dto.interview.AnswerEvaluationResponse;
import com.hiresense.dto.interview.AnswerSubmitRequest;
import com.hiresense.dto.interview.InterviewQuestionResponse;
import com.hiresense.dto.interview.InterviewResultResponse;
import com.hiresense.dto.interview.InterviewSessionResponse;
import com.hiresense.dto.interview.InterviewSetupRequest;
import com.hiresense.dto.interview.QuestionResultResponse;
import com.hiresense.entity.AnswerEvaluation;
import com.hiresense.entity.InterviewAnswer;
import com.hiresense.entity.InterviewQuestion;
import com.hiresense.entity.InterviewSession;
import com.hiresense.entity.JobRole;
import com.hiresense.entity.User;
import com.hiresense.enums.DifficultyLevel;
import com.hiresense.enums.InterviewStatus;
import com.hiresense.enums.ProgressMetric;
import com.hiresense.enums.QuestionCategory;
import com.hiresense.exception.BadRequestException;
import com.hiresense.exception.ConflictException;
import com.hiresense.exception.ResourceNotFoundException;
import com.hiresense.repository.AnswerEvaluationRepository;
import com.hiresense.repository.InterviewAnswerRepository;
import com.hiresense.repository.InterviewQuestionRepository;
import com.hiresense.repository.InterviewSessionRepository;
import com.hiresense.repository.JobRoleRepository;
import com.hiresense.service.ai.AiService;
import com.hiresense.service.ai.dto.AiAnswerEvaluation;
import com.hiresense.service.ai.dto.AiQuestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final JobRoleRepository jobRoleRepository;
    private final AiService aiService;
    private final ProgressService progressService;

    public InterviewService(InterviewSessionRepository sessionRepository,
                            InterviewQuestionRepository questionRepository,
                            InterviewAnswerRepository answerRepository,
                            AnswerEvaluationRepository evaluationRepository,
                            JobRoleRepository jobRoleRepository,
                            AiService aiService,
                            ProgressService progressService) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.evaluationRepository = evaluationRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.aiService = aiService;
        this.progressService = progressService;
    }

    @Transactional
    public InterviewSessionResponse setup(User user, InterviewSetupRequest request) {
        JobRole role = jobRoleRepository.findById(request.jobRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Job role not found with id " + request.jobRoleId()));

        int count = request.numberOfQuestions() == null ? 5 : clamp(request.numberOfQuestions(), 3, 20);
        DifficultyLevel difficulty = request.difficulty() == null ? DifficultyLevel.INTERMEDIATE : request.difficulty();

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setJobRole(role);
        session.setStatus(InterviewStatus.CONFIGURED);
        session.setDifficultyLevel(difficulty);
        session.setNumberOfQuestions(count);
        session.setTimeLimitSeconds(request.timeLimitSeconds());
        session.setCurrentQuestionIndex(0);

        List<String> focusSkills = role.getRequiredSkills().stream()
                .map(s -> s.getName())
                .toList();
        List<AiQuestion> generated = aiService.generateQuestions(
                role.getTitle(), count, difficulty.name().toLowerCase(Locale.ROOT), focusSkills);

        List<InterviewQuestion> questions = new ArrayList<>();
        int idx = 0;
        for (AiQuestion aiQuestion : generated) {
            InterviewQuestion q = new InterviewQuestion();
            q.setSession(session);
            q.setContent(aiQuestion.question());
            q.setCategory(parseCategory(aiQuestion.category()));
            q.setDifficulty(difficulty);
            q.setOrderIndex(idx++);
            q.setExpectedKeywords(joinKeywords(aiQuestion.expectedKeywords()));
            questions.add(q);
        }
        session.setQuestions(questions);
        session = sessionRepository.save(session);
        return toSessionResponse(session);
    }

    @Transactional
    public InterviewSessionResponse start(User user, Long sessionId) {
        InterviewSession session = findOwned(user, sessionId);
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("This interview has already been completed");
        }
        if (session.getStatus() == InterviewStatus.CONFIGURED) {
            session.setStatus(InterviewStatus.IN_PROGRESS);
            session.setStartedAt(Instant.now());
            session.setCurrentQuestionIndex(0);
            session = sessionRepository.save(session);
        }
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getUserSessions(User user) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(User user, Long sessionId) {
        return toSessionResponse(findOwned(user, sessionId));
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getQuestions(User user, Long sessionId) {
        InterviewSession session = findOwned(user, sessionId);
        return questionRepository.findBySessionIdOrderByOrderIndexAsc(session.getId())
                .stream()
                .map(q -> new InterviewQuestionResponse(
                        q.getId(),
                        session.getId(),
                        q.getContent(),
                        q.getCategory(),
                        q.getDifficulty(),
                        q.getOrderIndex(),
                        q.getAnswer() != null))
                .toList();
    }

    @Transactional
    public AnswerEvaluationResponse submitAnswer(User user, Long sessionId, Long questionId, AnswerSubmitRequest request) {
        InterviewSession session = findOwned(user, sessionId);
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("This interview has already been completed");
        }
        if (session.getStatus() == InterviewStatus.CONFIGURED) {
            session.setStatus(InterviewStatus.IN_PROGRESS);
            session.setStartedAt(Instant.now());
        }

        InterviewQuestion question = session.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in this interview"));
        if (question.getAnswer() != null) {
            throw new ConflictException("This question has already been answered");
        }

        String roleTitle = session.getJobRole() == null ? "general" : session.getJobRole().getTitle();
        AiAnswerEvaluation ai = aiService.evaluateAnswer(
                roleTitle, question.getContent(), question.getExpectedKeywords(), request.text());

        InterviewAnswer answer = new InterviewAnswer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setText(request.text());
        answer.setTimeTakenSeconds(request.timeTakenSeconds());
        answerRepository.save(answer);
        question.setAnswer(answer);

        AnswerEvaluation evaluation = new AnswerEvaluation();
        evaluation.setAnswer(answer);
        evaluation.setSession(session);
        evaluation.setScore(clamp(ai.score()));
        evaluation.setRelevanceScore(clamp(ai.relevanceScore()));
        evaluation.setClarityScore(clamp(ai.clarityScore()));
        evaluation.setDepthScore(clamp(ai.depthScore()));
        evaluation.setTopic(ai.topic());
        evaluation.setFeedback(ai.feedback());
        evaluation.setStrengths(joinKeywords(ai.strengths()));
        evaluation.setImprovements(joinKeywords(ai.improvements()));
        evaluation.setMissingKeywords(joinKeywords(ai.missingKeywords()));
        evaluation.setSuggestedAnswer(ai.suggestedAnswer());
        answer.setEvaluation(evaluation);
        evaluationRepository.save(evaluation);

        long answeredCount = questionRepository.countBySessionIdAndAnswerIsNotNull(session.getId());
        session.setCurrentQuestionIndex((int) answeredCount);
        if (answeredCount >= session.getQuestions().size()) {
            completeSession(session);
        } else {
            sessionRepository.save(session);
        }

        progressService.record(user, ProgressMetric.QUESTIONS_ANSWERED, clamp(ai.score()),
                ai.topic() == null || ai.topic().isBlank() ? "Interview answer" : ai.topic(),
                "Question " + (answeredCount) + " of " + session.getQuestions().size());

        return toEvaluationResponse(answer, question, evaluation);
    }

    @Transactional
    public InterviewResultResponse getResult(User user, Long sessionId) {
        InterviewSession session = findOwned(user, sessionId);
        if (session.getStatus() != InterviewStatus.COMPLETED) {
            throw new BadRequestException("The interview is not completed yet");
        }
        List<AnswerEvaluation> evaluations = evaluationRepository.findBySession(session);

        int overall = evaluations.isEmpty()
                ? 0
                : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluation::getScore).average().orElse(0));

        List<QuestionResultResponse> results = new ArrayList<>();
        for (AnswerEvaluation evaluation : evaluations) {
            InterviewAnswer answer = evaluation.getAnswer();
            InterviewQuestion question = answer.getQuestion();
            results.add(new QuestionResultResponse(
                    question.getId(),
                    question.getContent(),
                    question.getCategory() == null ? "GENERAL" : question.getCategory().name(),
                    answer.getText(),
                    evaluation.getScore(),
                    evaluation.getFeedback(),
                    splitList(evaluation.getStrengths()),
                    splitList(evaluation.getImprovements()),
                    splitList(evaluation.getMissingKeywords()),
                    evaluation.getSuggestedAnswer()));
        }

        String summary = overall >= 80
                ? "Outstanding performance. You are well prepared for this role."
                : overall >= 60
                ? "Good performance. Review the weaker areas to improve further."
                : "Keep practicing. Start with the suggested learning plan for the weakest topics.";

        String roleTitle = session.getJobRole() == null ? "General" : session.getJobRole().getTitle();
        return new InterviewResultResponse(session.getId(), roleTitle, overall, summary, results);
    }

    private void completeSession(InterviewSession session) {
        List<AnswerEvaluation> evaluations = evaluationRepository.findBySession(session);
        int overall = evaluations.isEmpty()
                ? 0
                : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluation::getScore).average().orElse(0));
        session.setStatus(InterviewStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);
        progressService.record(session.getUser(), ProgressMetric.INTERVIEW_SCORE, overall,
                session.getJobRole() == null ? "Interview" : session.getJobRole().getTitle(),
                weakTopicSummary(evaluations));
    }

    private String weakTopicSummary(List<AnswerEvaluation> evaluations) {
        List<String> weak = evaluations.stream()
                .filter(e -> e.getTopic() != null && !e.getTopic().isBlank() && e.getScore() < 70)
                .map(AnswerEvaluation::getTopic)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
        return weak.isEmpty() ? "No weak topics detected" : "Weak areas: " + String.join(", ", weak);
    }

    private InterviewSession findOwned(User user, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with id " + sessionId));
    }

    private QuestionCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return QuestionCategory.TECHNICAL;
        }
        try {
            return QuestionCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return QuestionCategory.TECHNICAL;
        }
    }

    private String joinKeywords(List<String> keywords) {
        if (keywords == null) {
            return null;
        }
        return String.join(",", keywords);
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private AnswerEvaluationResponse toEvaluationResponse(InterviewAnswer answer,
                                                         InterviewQuestion question,
                                                         AnswerEvaluation evaluation) {
        return new AnswerEvaluationResponse(
                answer.getId(),
                question.getId(),
                question.getContent(),
                evaluation.getScore(),
                evaluation.getRelevanceScore(),
                evaluation.getClarityScore(),
                evaluation.getDepthScore(),
                evaluation.getFeedback(),
                evaluation.getTopic(),
                splitList(evaluation.getStrengths()),
                splitList(evaluation.getImprovements()),
                splitList(evaluation.getMissingKeywords()),
                evaluation.getSuggestedAnswer());
    }

    private InterviewSessionResponse toSessionResponse(InterviewSession session) {
        String roleTitle = session.getJobRole() == null ? null : session.getJobRole().getTitle();
        Long roleId = session.getJobRole() == null ? null : session.getJobRole().getId();
        return new InterviewSessionResponse(
                session.getId(),
                roleId,
                roleTitle,
                session.getStatus(),
                session.getDifficultyLevel(),
                session.getNumberOfQuestions(),
                session.getTimeLimitSeconds(),
                session.getCurrentQuestionIndex(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}