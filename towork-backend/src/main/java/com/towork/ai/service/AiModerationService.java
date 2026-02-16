package com.towork.ai.service;

import com.towork.ai.config.AiProperties;
import com.towork.ai.dto.AiModerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModerationService {

    private final AiFeatureService aiFeatureService;
    private final AiProperties properties;

    public AiModerationResponse moderate(String content) {
        return aiFeatureService.moderateText(content);
    }

    public boolean shouldBlock(AiModerationResponse response) {
        if (response == null || response.getFlagged() == null) {
            return false;
        }
        if (!properties.getModeration().isBlock()) {
            return false;
        }
        double score = response.getScore() != null ? response.getScore() : 0.0;
        return Boolean.TRUE.equals(response.getFlagged()) && score >= properties.getModeration().getBlockScore();
    }
}
