package com.hiresense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullInterviewLifecycle() throws Exception {
        String token = registerAndLogin();

        // Pick the first seeded job role
        String rolesJson = mockMvc.perform(get("/api/job-roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode roles = objectMapper.readTree(rolesJson);
        assertThat(roles.size()).isGreaterThan(0);
        long jobRoleId = roles.get(0).get("id").asLong();

        // Setup a 3-question mock interview
        String setupBody = """
                {"jobRoleId":%d,"numberOfQuestions":3,"difficulty":"INTERMEDIATE"}
                """.formatted(jobRoleId);
        String setupJson = mockMvc.perform(post("/api/interviews/setup")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setupBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numberOfQuestions").value(3))
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(setupJson).get("id").asLong();

        // Start the interview
        mockMvc.perform(post("/api/interviews/" + sessionId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Fetch questions
        String questionsJson = mockMvc.perform(get("/api/interviews/" + sessionId + "/questions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        JsonNode questions = objectMapper.readTree(questionsJson);

        // Answer every question and expect AI evaluations back
        int index = 0;
        for (JsonNode question : questions) {
            long questionId = question.get("id").asLong();
            String answerBody = """
                    {"text":"I would approach this by carefully analyzing the requirements, applying my knowledge of \
                    the core concepts, and validating the solution with tests and examples.","timeTakenSeconds":45}
                    """;
            mockMvc.perform(post("/api/interviews/" + sessionId + "/questions/" + questionId + "/answer")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(answerBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.score").isNumber())
                    .andExpect(jsonPath("$.feedback").isNotEmpty());

            // Answering the same question twice must fail while the interview is still active
            if (index == 0) {
                String duplicateBody = """
                        {"text":"Repeated answer attempt.","timeTakenSeconds":10}
                        """;
                mockMvc.perform(post("/api/interviews/" + sessionId + "/questions/" + questionId + "/answer")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(duplicateBody))
                        .andExpect(status().isConflict());
            }
            index++;
        }

        // Result should be available and include evaluations for all questions
        mockMvc.perform(get("/api/interviews/" + sessionId + "/result")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.jobRoleTitle").isNotEmpty());
    }

    private String registerAndLogin() throws Exception {
        String email = "interviewer-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"fullName":"Interview Tester","email":"%s","password":"StrongPass123!"}
                """.formatted(email);
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}