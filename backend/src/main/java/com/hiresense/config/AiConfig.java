package com.hiresense.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.service.ai.AiResponseParser;
import com.hiresense.service.ai.AiService;
import com.hiresense.service.ai.MockAiService;
import com.hiresense.service.ai.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public AiService aiService(
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.ai.openai.api-key:}") String apiKey,
            @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.openai.temperature:0.3}") double temperature,
            @Value("${app.ai.openai.max-tokens:2000}") int maxTokens,
            @Value("${app.ai.openai.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper,
            AiResponseParser parser) {

        String normalized = provider == null ? "mock" : provider.toLowerCase();

        if ("openai".equals(normalized)) {
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("AI_PROVIDER=openai but OPENAI_API_KEY is not set - falling back to the mock AI provider. "
                        + "The application will keep working with deterministic responses.");
                return new MockAiService();
            }
            log.info("Using OpenAI AI provider (model={})", model);
            return new OpenAiService(apiKey, baseUrl, model, temperature, maxTokens, timeoutSeconds,
                    objectMapper, parser);
        }

        log.info("Using mock AI provider (AI_PROVIDER=mock). Set OPENAI_API_KEY and AI_PROVIDER=openai to enable real AI.");
        return new MockAiService();
    }
}