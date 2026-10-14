package com.hiresense.service.ai;

import com.hiresense.service.ai.dto.AiAnswerEvaluation;
import com.hiresense.service.ai.dto.AiQuestion;
import com.hiresense.service.ai.dto.AiResumeAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiServiceTest {

    private final MockAiService service = new MockAiService();

    @Test
    void extractsKnownSkillsFromResumeText() {
        String resumeText = "Experience with Java, Spring Boot, PostgreSQL, Docker and REST APIs.";
        List<String> skills = service.extractSkills(resumeText);
        assertThat(skills).contains("Java", "Spring Boot", "PostgreSQL", "Docker", "REST APIs");
    }

    @Test
    void generatesRequestedNumberOfQuestions() {
        List<AiQuestion> questions = service.generateQuestions("Java Backend Developer", 3, "intermediate",
                List.of("Spring Boot", "PostgreSQL"));
        assertThat(questions).hasSize(3);
        assertThat(questions).allSatisfy(q -> {
            assertThat(q.question()).isNotBlank();
            assertThat(q.expectedKeywords()).isNotEmpty();
        });
    }

    @Test
    void evaluatesAnswersWithScoresInRange() {
        AiAnswerEvaluation evaluation = service.evaluateAnswer(
                "Java Backend Developer",
                "Explain Spring Boot and REST APIs",
                "spring boot,rest,http",
                "Spring Boot simplifies building production ready REST APIs. It auto-configures components and "
                        + "provides embedded servers. REST uses HTTP methods like GET and POST to access resources.");
        assertThat(evaluation.score()).isBetween(0, 100);
        assertThat(evaluation.relevanceScore()).isBetween(0, 100);
        assertThat(evaluation.feedback()).isNotBlank();
        assertThat(evaluation.missingKeywords()).hasSize(0);
    }

    @Test
    void detectsMissingKeywords() {
        AiAnswerEvaluation evaluation = service.evaluateAnswer(
                "DevOps Engineer",
                "Explain Docker containers",
                "docker,container,image,kubernetes",
                "I use Docker to run containers locally.");
        assertThat(evaluation.missingKeywords()).isNotEmpty();
        assertThat(evaluation.missingKeywords()).contains("kubernetes");
    }

    @Test
    void analyzesResumeWithAtsScore() {
        AiResumeAnalysis analysis = service.analyzeResume(
                "Java developer skilled in Spring Boot, PostgreSQL, Docker, REST APIs.",
                List.of("Java", "Spring Boot", "PostgreSQL", "Kubernetes"));
        assertThat(analysis.atsScore()).isBetween(0, 100);
        assertThat(analysis.keywordMatchScore()).isBetween(0, 100);
        assertThat(analysis.extractedSkills()).contains("Spring Boot");
        assertThat(analysis.summary()).isNotBlank();
    }

    @Test
    void generatesLearningPlanFromWeakSkills() {
        var plan = service.generateLearningPlan("Java Backend Developer",
                List.of("Kafka", "System Design"), "Some context");
        assertThat(plan.title()).contains("Java Backend Developer");
        assertThat(plan.items()).isNotEmpty();
        assertThat(plan.items()).extracting(item -> item.skillName()).contains("Kafka");
    }
}