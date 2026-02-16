package com.towork.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = false;
    private OpenAi openai = new OpenAi();
    private Moderation moderation = new Moderation();
    private Features features = new Features();

    @Data
    public static class OpenAi {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String chatModel = "gpt-4o-mini";
        private String embeddingModel = "text-embedding-3-small";
        private double temperature = 0.2;
        private int maxTokens = 800;
        private int timeoutSeconds = 20;
        private int maxInputChars = 6000;
    }

    @Data
    public static class Moderation {
        private boolean enabled = true;
        private boolean block = false;
        private double blockScore = 0.75;
    }

    @Data
    public static class Features {
        private boolean matching = true;
        private boolean domaineSuggestion = true;
        private boolean drafting = true;
        private boolean summarization = true;
        private boolean resumeExtraction = true;
        private boolean moderation = true;
    }
}
