package com.hiresense.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiresense.exception.AiServiceException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Extracts and validates structured JSON returned by an AI model. Handles
 * markdown code fences and leading prose so responses remain usable even when
 * the provider returns slightly malformed output.
 */
@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractJson(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new AiServiceException("AI returned an empty response");
        }
        String cleaned = rawContent.trim();
        // Strip markdown code fences
        cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\s*", "");
        cleaned = cleaned.replaceAll("(?s)\\s*```$", "");
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        int arrStart = cleaned.indexOf('[');
        int arrEnd = cleaned.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            return cleaned.substring(arrStart, arrEnd + 1);
        }
        throw new AiServiceException("AI response was not valid JSON: " + truncate(cleaned));
    }

    public <T> T parseJsonObject(String rawContent, Class<T> targetType) {
        String json = extractJson(rawContent);
        try {
            T value = objectMapper.readValue(json, targetType);
            validateNotNull(value, targetType.getSimpleName());
            return value;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Failed to parse AI JSON response for "
                    + targetType.getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> parseJsonObject(String rawContent) {
        String json = extractJson(rawContent);
        try {
            Map<String, Object> value = objectMapper.readValue(json, Map.class);
            if (value == null || value.isEmpty()) {
                throw new AiServiceException("AI response contained an empty object");
            }
            return value;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Failed to parse AI JSON response: " + ex.getMessage(), ex);
        }
    }

    public static void validateNotNull(Object value, String label) {
        if (value == null) {
            throw new AiServiceException("AI response was missing required field: " + label);
        }
        if (value instanceof String s && s.isBlank()) {
            throw new AiServiceException("AI response contained a blank value for: " + label);
        }
        if (value instanceof Collection<?> c && c.isEmpty()) {
            throw new AiServiceException("AI response contained an empty list for: " + label);
        }
    }

    private String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}