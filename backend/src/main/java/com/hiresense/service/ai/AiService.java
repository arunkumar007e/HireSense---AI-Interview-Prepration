package com.hiresense.service.ai;

import com.hiresense.service.ai.dto.AiAnswerEvaluation;
import com.hiresense.service.ai.dto.AiLearningPlan;
import com.hiresense.service.ai.dto.AiQuestion;
import com.hiresense.service.ai.dto.AiResumeAnalysis;
import com.hiresense.service.ai.dto.AiSkillGap;

import java.util.List;

/**
 * Abstraction over the external AI provider so the rest of the application is
 * decoupled from a specific vendor. Implementations must return structured
 * data; providers such as OpenAI ship with a validating JSON parser, while the
 * mock implementation provides deterministic fallback behaviour so the platform
 * works without any API key.
 */
public interface AiService {

    String providerName();

    AiResumeAnalysis analyzeResume(String resumeText, List<String> targetSkills);

    List<String> extractSkills(String resumeText);

    AiSkillGap analyzeSkillGap(List<String> resumeSkills,
                               List<String> requiredSkills,
                               String jobRoleTitle);

    List<AiQuestion> generateQuestions(String jobRoleTitle,
                                       int count,
                                       String difficulty,
                                       List<String> focusSkills);

    AiAnswerEvaluation evaluateAnswer(String jobRoleTitle,
                                      String question,
                                      String expectedKeywords,
                                      String userAnswer);

    AiLearningPlan generateLearningPlan(String jobRoleTitle,
                                        List<String> weakSkills,
                                        String contextSummary);
}