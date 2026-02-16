package com.towork.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towork.ai.client.AiClient;
import com.towork.ai.util.AiJsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTextService {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public String chat(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens) {
        return aiClient.chat(systemPrompt, userPrompt, temperature, maxTokens);
    }

    public <T> T chatJson(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens, Class<T> type) {
        String content = aiClient.chatJson(systemPrompt, userPrompt, temperature, maxTokens);
        return parseJson(content, type);
    }

    public <T> T chatJson(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens, TypeReference<T> type) {
        String content = aiClient.chatJson(systemPrompt, userPrompt, temperature, maxTokens);
        return parseJson(content, type);
    }

    private <T> T parseJson(String content, Class<T> type) {
        String json = AiJsonUtils.extractJson(content);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid AI JSON response: " + content, ex);
        }
    }

    private <T> T parseJson(String content, TypeReference<T> type) {
        String json = AiJsonUtils.extractJson(content);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid AI JSON response: " + content, ex);
        }
    }
}
