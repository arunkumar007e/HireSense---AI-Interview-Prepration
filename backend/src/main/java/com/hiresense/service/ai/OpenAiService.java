package com.hiresense.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.exception.AiServiceException;
import com.hiresense.service.ai.dto.AiAnswerEvaluation;
import com.hiresense.service.ai.dto.AiLearningPlan;
import com.hiresense.service.ai.dto.AiQuestion;
import com.hiresense.service.ai.dto.AiResumeAnalysis;
import com.hiresense.service.ai.dto.AiSkillGap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion implementation of {@link AiService}. Each
 * method builds a focused prompt that instructs the model to answer with strict
 * JSON only; responses are parsed and validated before being returned.
 */
public class OpenAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final String SYSTEM_PROMPT =
            "You are HireSense, an expert technical interviewer and career coach. "
            + "Always respond with strict JSON only. Do not include markdown, prose, or code fences.";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiResponseParser parser;

    public OpenAiService(String apiKey, String baseUrl, String model,
                         double temperature, int maxTokens, int timeoutSeconds,
                         ObjectMapper objectMapper, AiResponseParser parser) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public AiResumeAnalysis analyzeResume(String resumeText, List<String> targetSkills) {
        String prompt = """
                Analyze the following resume text for a job application.
                Return JSON exactly in this shape:
                {"atsScore": 0, "keywordMatchScore": 0, "extractedSkills": [], "summary": "", "strengths": [], "improvements": []}
                atsScore and keywordMatchScore must be integers between 0 and 100.
                Target skills: %s
                Resume text:
                %s
                """.formatted(String.join(", ", nullSafe(targetSkills)), snippet(resumeText));
        Map<String, Object> json = requestJson(prompt);
        try {
            JsonNodeMap node = new JsonNodeMap(json);
            return new AiResumeAnalysis(
                    node.integer("atsScore"),
                    node.integer("keywordMatchScore"),
                    node.stringList("extractedSkills"),
                    node.string("summary"),
                    node.stringList("strengths"),
                    node.stringList("improvements"));
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Invalid resume analysis from AI provider", ex);
        }
    }

    @Override
    public List<String> extractSkills(String resumeText) {
        String prompt = """
                Extract all skills mentioned in the resume below.
                Return JSON exactly in this shape:
                {"skills": []}
                Resume text:
                %s
                """.formatted(snippet(resumeText));
        Map<String, Object> json = requestJson(prompt);
        return JsonNodeMap.stringList(json, "skills");
    }

    @Override
    public AiSkillGap analyzeSkillGap(List<String> resumeSkills, List<String> requiredSkills, String jobRoleTitle) {
        String prompt = """
                Compare the candidate's skills against the skills required for a %s role.
                Return JSON exactly in this shape:
                {"matchScore": 0, "summary": "", "skills": [{"skill": "", "requiredLevel": "advanced", "currentLevel": "beginner", "gap": "none|minor|major", "explanation": ""}]}
                matchScore must be an integer between 0 and 100, one entry per required skill.
                Candidate skills: %s
                Required skills: %s
                """.formatted(jobRoleTitle, String.join(", ", nullSafe(resumeSkills)),
                String.join(", ", nullSafe(requiredSkills)));
        Map<String, Object> json = requestJson(prompt);
        try {
            return new AiSkillGap(
                    ((Number) json.getOrDefault("matchScore", 0)).intValue(),
                    String.valueOf(json.getOrDefault("summary", "")),
                    new JsonNodeMap(json).objectList("skills")
                            .stream()
                            .map(map -> new com.hiresense.service.ai.dto.AiSkillGapItem(
                                    String.valueOf(map.get("skill")),
                                    String.valueOf(map.get("requiredLevel")),
                                    String.valueOf(map.get("currentLevel")),
                                    String.valueOf(map.get("gap")),
                                    String.valueOf(map.get("explanation"))))
                            .toList());
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Invalid skill gap analysis from AI provider", ex);
        }
    }

    @Override
    public List<AiQuestion> generateQuestions(String jobRoleTitle, int count, String difficulty, List<String> focusSkills) {
        String prompt = """
                Generate %d interview questions for a %s role at difficulty "%s".
                Mix technical, behavioral, and problem-solving categories.
                Focus skills: %s
                Return JSON exactly in this shape:
                {"questions": [{"question": "", "category": "TECHNICAL", "difficulty": "intermediate", "expectedKeywords": []}]}
                """.formatted(count, jobRoleTitle, difficulty, String.join(", ", nullSafe(focusSkills)));
        Map<String, Object> json = requestJson(prompt);
        try {
            List<AiQuestion> questions = new JsonNodeMap(json).objectList("questions")
                    .stream()
                    .map(map -> new AiQuestion(
                            String.valueOf(map.get("question")),
                            String.valueOf(map.get("category")),
                            String.valueOf(map.get("difficulty")),
                            JsonNodeMap.stringList(map, "expectedKeywords")))
                    .toList();
            if (questions.isEmpty()) {
                throw new AiServiceException("AI provider returned no questions");
            }
            return questions;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Invalid question list from AI provider", ex);
        }
    }

    @Override
    public AiAnswerEvaluation evaluateAnswer(String jobRoleTitle, String question, String expectedKeywords, String userAnswer) {
        String prompt = """
                Evaluate the candidate answer for a %s interview.
                Question: %s
                Expected keywords: %s
                Candidate answer: %s
                Return JSON exactly in this shape:
                {"topic": "", "score": 0, "relevanceScore": 0, "clarityScore": 0, "depthScore": 0, "feedback": "", "strengths": [], "improvements": [], "missingKeywords": [], "suggestedAnswer": ""}
                All scores must be integers between 0 and 100.
                """.formatted(jobRoleTitle, question, expectedKeywords, snippet(userAnswer));
        Map<String, Object> json = requestJson(prompt);
        try {
            JsonNodeMap node = new JsonNodeMap(json);
            return new AiAnswerEvaluation(
                    node.integer("score"),
                    node.integer("relevanceScore"),
                    node.integer("clarityScore"),
                    node.integer("depthScore"),
                    node.string("feedback"),
                    node.string("topic"),
                    node.stringList("strengths"),
                    node.stringList("improvements"),
                    node.stringList("missingKeywords"),
                    node.string("suggestedAnswer"));
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Invalid answer evaluation from AI provider", ex);
        }
    }

    @Override
    public AiLearningPlan generateLearningPlan(String jobRoleTitle, List<String> weakSkills, String contextSummary) {
        String prompt = """
                Create a personalized learning plan for a candidate targeting a %s role.
                Weak skills to improve: %s
                Context: %s
                Return JSON exactly in this shape:
                {"title": "", "description": "", "items": [{"title": "", "description": "", "resourceUrl": "", "skillName": "", "orderIndex": 1, "targetDays": 5}]}
                Provide 5 to 8 items ordered by priority.
                """.formatted(jobRoleTitle, String.join(", ", nullSafe(weakSkills)),
                contextSummary == null ? "" : snippet(contextSummary));
        Map<String, Object> json = requestJson(prompt);
        try {
            JsonNodeMap node = new JsonNodeMap(json);
            List<com.hiresense.service.ai.dto.AiLearningPlanItem> items =
                    node.objectList("items").stream()
                            .map(map -> new com.hiresense.service.ai.dto.AiLearningPlanItem(
                                    String.valueOf(map.get("title")),
                                    String.valueOf(map.get("description")),
                                    String.valueOf(map.get("resourceUrl")),
                                    String.valueOf(map.get("skillName")),
                                    ((Number) map.getOrDefault("orderIndex", 0)).intValue(),
                                    ((Number) map.getOrDefault("targetDays", 5)).intValue()))
                            .toList();
            if (items.isEmpty()) {
                throw new AiServiceException("AI provider returned no learning plan items");
            }
            return new AiLearningPlan(node.string("title"), node.string("description"), items);
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Invalid learning plan from AI provider", ex);
        }
    }

    private Map<String, Object> requestJson(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("OpenAI API key is not configured. Set OPENAI_API_KEY or use AI_PROVIDER=mock");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ));
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("AI provider returned HTTP {}: {}", response.statusCode(), snippet(response.body()));
                throw new AiServiceException("AI provider request failed with status " + response.statusCode());
            }
            Map<String, Object> root = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = JsonNodeMap.objectList(root, "choices");
            if (choices.isEmpty()) {
                throw new AiServiceException("AI provider returned no completions");
            }
            Object message = choices.get(0).get("message");
            if (!(message instanceof Map<?, ?> msg)) {
                throw new AiServiceException("AI provider response had no message");
            }
            String content = String.valueOf(((Map<?, ?>) msg).get("content"));
            return parser.parseJsonObject(content);
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("AI provider call failed: " + ex.getMessage(), ex);
        }
    }

    private List<String> nullSafe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 6000 ? text.substring(0, 6000) : text;
    }

    /** Minimal null-safe JSON tree helper. */
    private static final class JsonNodeMap {

        private final Map<String, Object> map;

        JsonNodeMap(Map<String, Object> map) {
            this.map = map;
        }

        static List<Map<String, Object>> objectList(Map<String, Object> root, String key) {
            Object value = root.get(key);
            if (value instanceof List<?> list) {
                List<Map<String, Object>> result = new java.util.ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        result.add(toMap(m));
                    }
                }
                return result;
            }
            return List.of();
        }

        static List<String> stringList(Map<String, Object> root, String key) {
            Object value = root.get(key);
            if (value instanceof List<?> list) {
                List<String> result = new java.util.ArrayList<>();
                for (Object item : list) {
                    result.add(String.valueOf(item));
                }
                return result;
            }
            return List.of();
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> toMap(Map<?, ?> source) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            source.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }

        List<Map<String, Object>> objectList(String key) {
            return objectList(map, key);
        }

        List<String> stringList(String key) {
            return stringList(map, key);
        }

        String string(String key) {
            Object value = map.get(key);
            AiResponseParser.validateNotNull(value, key);
            return String.valueOf(value);
        }

        int integer(String key) {
            Object value = map.get(key);
            AiResponseParser.validateNotNull(value, key);
            return ((Number) value).intValue();
        }
    }
}