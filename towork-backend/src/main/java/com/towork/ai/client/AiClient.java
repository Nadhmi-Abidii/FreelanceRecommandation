package com.towork.ai.client;

import java.util.List;

public interface AiClient {
    String chat(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens);

    String chatJson(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens);

    List<Double> embed(String input);
}
