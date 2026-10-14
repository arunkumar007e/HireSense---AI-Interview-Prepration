package com.hiresense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeAndGapFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadAnalyzeGapAndGenerateLearningPlan() throws Exception {
        String token = register();

        String rolesJson = mockMvc.perform(get("/api/job-roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode roles = objectMapper.readTree(rolesJson);
        assertThat(roles.size()).isGreaterThan(0);
        long jobRoleId = roles.get(0).get("id").asLong();

        // Upload a plain-text resume
        String resumeText = """
                Software Engineer with 5 years of experience.
                Skills: Java, Spring Boot, PostgreSQL, Docker, REST APIs, Git, JUnit.
                Built microservices for an e-commerce platform using Hibernate and Kafka.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt",
                "text/plain", resumeText.getBytes(StandardCharsets.UTF_8));
        String uploadJson = mockMvc.perform(multipart("/api/resumes/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.analysisStatus").value("READY"))
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(uploadJson).get("id").asLong();

        // Analyse against the selected job role
        String analyzeBody = "{\"jobRoleId\":%d}".formatted(jobRoleId);
        mockMvc.perform(post("/api/resumes/" + resumeId + "/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analyzeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atsScore").isNumber())
                .andExpect(jsonPath("$.status").value("ANALYZED"))
                .andExpect(jsonPath("$.extractedSkills").isArray());

        // Skill gap analysis
        String gapBody = "{\"resumeSkills\":[\"Java\",\"Spring Boot\",\"PostgreSQL\",\"Docker\",\"REST APIs\"]}";
        mockMvc.perform(post("/api/job-roles/" + jobRoleId + "/skill-gap")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gapBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").isNumber())
                .andExpect(jsonPath("$.skills").isArray());

        // Generate a learning plan
        String planBody = "{\"jobRoleId\":%d,\"resumeId\":%d}".formatted(jobRoleId, resumeId);
        String planJson = mockMvc.perform(post("/api/learning-plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode plan = objectMapper.readTree(planJson);
        assertThat(plan.get("items").size()).isGreaterThan(0);

        // Complete the first item and confirm progress updates
        long itemId = plan.get("items").get(0).get("id").asLong();
        long planId = plan.get("id").asLong();
        mockMvc.perform(post("/api/learning-plans/" + planId + "/items/" + itemId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedItems").value(1));

        // Progress summary reflects the activity
        mockMvc.perform(get("/api/progress/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningPlansCreated").value(1))
                .andExpect(jsonPath("$.questionsAnswered").value(0));
    }

    private String register() throws Exception {
        String email = "resume-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"fullName":"Resume Tester","email":"%s","password":"StrongPass123!"}
                """.formatted(email);
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }
}