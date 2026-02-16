package com.towork.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.towork.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient implements AiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient aiHttpClient;

    @Override
    public String chat(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens) {
        return chatInternal(systemPrompt, userPrompt, temperature, maxTokens, false);
    }

    @Override
    public String chatJson(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens) {
        return chatInternal(systemPrompt, userPrompt, temperature, maxTokens, true);
    }

    @Override
    public List<Double> embed(String input) {
        ensureEnabled();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getOpenai().getEmbeddingModel());
        payload.put("input", input);

        String responseBody = post("/embeddings", payload);
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new AiException("OpenAI embeddings response missing data");
            }
            JsonNode embedding = data.get(0).path("embedding");
            if (!embedding.isArray()) {
                throw new AiException("OpenAI embeddings response missing embedding");
            }
            return objectMapper.convertValue(embedding, objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception ex) {
            throw new AiException("Failed to parse OpenAI embeddings response", ex);
        }
    }

    private String chatInternal(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens, boolean jsonMode) {
        ensureEnabled();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getOpenai().getChatModel());
        payload.put("temperature", temperature != null ? temperature : properties.getOpenai().getTemperature());
        payload.put("max_tokens", maxTokens != null ? maxTokens : properties.getOpenai().getMaxTokens());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (jsonMode) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        String responseBody = post("/chat/completions", payload);
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new AiException("OpenAI response missing content");
            }
            return content.asText();
        } catch (Exception ex) {
            throw new AiException("Failed to parse OpenAI chat response", ex);
        }
    }

    private String post(String path, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOpenai().getBaseUrl() + path))
                    .timeout(Duration.ofSeconds(properties.getOpenai().getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new AiException("OpenAI request failed: HTTP " + response.statusCode() + " - " + response.body());
        } catch (AiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiException("OpenAI request failed", ex);
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new AiException("AI is disabled");
        }
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException("OpenAI API key is missing");
        }
    }
}
