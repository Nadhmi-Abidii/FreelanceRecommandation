package com.towork.ai.service;

import com.towork.ai.config.AiProperties;
import com.towork.ai.dto.AiDomainSuggestion;
import com.towork.ai.dto.AiDomainSuggestionRequest;
import com.towork.ai.dto.AiDraftRequest;
import com.towork.ai.dto.AiDraftResponse;
import com.towork.ai.dto.AiModerationResponse;
import com.towork.ai.dto.AiResumeExtractionResult;
import com.towork.ai.dto.AiRewriteRequest;
import com.towork.ai.dto.AiRewriteResponse;
import com.towork.ai.dto.AiSkillDto;
import com.towork.ai.dto.AiSummaryResponse;
import com.towork.user.entity.Domaine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiFeatureService {

    private final AiTextService aiTextService;
    private final AiProperties properties;

    public List<AiDomainSuggestion> suggestDomaines(AiDomainSuggestionRequest request, List<Domaine> domaines) {
        if (!properties.isEnabled() || !properties.getFeatures().isDomaineSuggestion()) {
            return fallbackDomaines(request, domaines);
        }

        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 3;
        String missionText = buildMissionText(request.getTitle(), request.getDescription(),
                request.getRequirements(), request.getSkillsRequired());
        if (missionText.isBlank()) {
            return List.of();
        }

        String language = normalizeLanguage(request.getLanguage());
        String systemPrompt = "You are an assistant that selects the most relevant domains from a provided list. Return only JSON.";
        String userPrompt = buildDomainSuggestionPrompt(missionText, domaines, limit, language);

        try {
            DomainSuggestionPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.2, 400, DomainSuggestionPayload.class);
            return normalizeDomainSuggestions(payload, domaines, limit);
        } catch (Exception ex) {
            log.warn("AI domain suggestion failed, using fallback: {}", ex.getMessage());
            return fallbackDomaines(request, domaines);
        }
    }

    public AiDraftResponse draftMission(AiDraftRequest request) {
        if (!properties.isEnabled() || !properties.getFeatures().isDrafting()) {
            return new AiDraftResponse(request.getTitle(), request.getDescription(),
                    request.getRequirements(), request.getSkillsRequired(), "AI drafting disabled");
        }

        String language = normalizeLanguage(request.getLanguage());
        String systemPrompt = "You are a writing assistant for freelance mission postings. Return only JSON.";
        String userPrompt = buildDraftPrompt(request, language);

        try {
            DraftPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.4, 700, DraftPayload.class);
            return new AiDraftResponse(payload.getTitle(), payload.getDescription(), payload.getRequirements(),
                    payload.getSkillsSuggested(), payload.getNotes());
        } catch (Exception ex) {
            log.warn("AI mission draft failed, returning original: {}", ex.getMessage());
            return new AiDraftResponse(request.getTitle(), request.getDescription(),
                    request.getRequirements(), request.getSkillsRequired(), "AI drafting failed");
        }
    }

    public AiRewriteResponse rewriteText(AiRewriteRequest request) {
        if (!properties.isEnabled() || !properties.getFeatures().isDrafting()) {
            return new AiRewriteResponse(request.getContent(), "AI rewrite disabled");
        }

        String language = normalizeLanguage(request.getLanguage());
        String systemPrompt = "You rewrite user text for clarity and tone. Return only JSON.";
        String userPrompt = buildRewritePrompt(request, language);

        try {
            RewritePayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.3, 500, RewritePayload.class);
            return new AiRewriteResponse(payload.getContent(), payload.getNotes());
        } catch (Exception ex) {
            log.warn("AI rewrite failed, returning original: {}", ex.getMessage());
            return new AiRewriteResponse(request.getContent(), "AI rewrite failed");
        }
    }

    public AiSummaryResponse summarizeMission(String title, String description, String requirements, String status, String language) {
        if (!properties.isEnabled() || !properties.getFeatures().isSummarization()) {
            return fallbackMissionSummary(title, description);
        }
        String missionText = buildMissionText(title, description, requirements, null);
        if (missionText.isBlank()) {
            return fallbackMissionSummary(title, description);
        }

        String systemPrompt = "You summarize missions and propose next steps. Return only JSON.";
        String userPrompt = buildMissionSummaryPrompt(missionText, status, normalizeLanguage(language));

        try {
            SummaryPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.2, 350, SummaryPayload.class);
            return new AiSummaryResponse(payload.getSummary(), safeList(payload.getNextSteps()));
        } catch (Exception ex) {
            log.warn("AI mission summary failed, using fallback: {}", ex.getMessage());
            return fallbackMissionSummary(title, description);
        }
    }

    public AiSummaryResponse summarizeConversation(List<String> messages, String context, String language) {
        if (!properties.isEnabled() || !properties.getFeatures().isSummarization()) {
            return fallbackConversationSummary(messages);
        }
        if (messages == null || messages.isEmpty()) {
            return new AiSummaryResponse("No messages to summarize.", List.of());
        }

        String systemPrompt = "You summarize a conversation and propose next steps. Return only JSON.";
        String userPrompt = buildConversationSummaryPrompt(messages, context, normalizeLanguage(language));

        try {
            SummaryPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.2, 400, SummaryPayload.class);
            return new AiSummaryResponse(payload.getSummary(), safeList(payload.getNextSteps()));
        } catch (Exception ex) {
            log.warn("AI conversation summary failed, using fallback: {}", ex.getMessage());
            return fallbackConversationSummary(messages);
        }
    }

    public AiModerationResponse moderateText(String content) {
        if (!properties.isEnabled() || !properties.getFeatures().isModeration() || !properties.getModeration().isEnabled()) {
            return new AiModerationResponse(false, 0.0, "OK", "Moderation disabled");
        }
        if (content == null || content.isBlank()) {
            return new AiModerationResponse(false, 0.0, "OK", "Empty content");
        }

        String systemPrompt = "You moderate content for spam and toxicity. Return only JSON.";
        String userPrompt = buildModerationPrompt(content);

        try {
            ModerationPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.0, 200, ModerationPayload.class);
            return new AiModerationResponse(payload.getFlagged(), payload.getScore(), payload.getLabel(), payload.getReason());
        } catch (Exception ex) {
            log.warn("AI moderation failed, allowing content: {}", ex.getMessage());
            return new AiModerationResponse(false, 0.0, "OK", "Moderation failed");
        }
    }

    public AiResumeExtractionResult extractSkillsFromResume(String resumeText, String language) {
        if (!properties.isEnabled() || !properties.getFeatures().isResumeExtraction()) {
            return new AiResumeExtractionResult("AI resume extraction disabled", List.of());
        }
        if (resumeText == null || resumeText.isBlank()) {
            return new AiResumeExtractionResult("No resume text", List.of());
        }

        String systemPrompt = "You extract structured skills from resume text. Return only JSON.";
        String userPrompt = buildResumePrompt(resumeText, normalizeLanguage(language));

        try {
            ResumePayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.2, 600, ResumePayload.class);
            return new AiResumeExtractionResult(payload.getSummary(), safeList(payload.getSkills()));
        } catch (Exception ex) {
            log.warn("AI resume extraction failed: {}", ex.getMessage());
            return new AiResumeExtractionResult("AI resume extraction failed", List.of());
        }
    }

    public String buildMissionText(String title, String description, String requirements, String skills) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Title", title);
        append(builder, "Description", description);
        append(builder, "Requirements", requirements);
        append(builder, "Skills", skills);
        return truncate(builder.toString());
    }

    private List<AiDomainSuggestion> normalizeDomainSuggestions(DomainSuggestionPayload payload, List<Domaine> domaines, int limit) {
        if (payload == null || payload.getSuggestions() == null) {
            return List.of();
        }
        Map<Long, Domaine> domaineById = domaines.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Domaine::getId, d -> d, (a, b) -> a));

        List<AiDomainSuggestion> suggestions = new ArrayList<>();
        for (DomainSuggestionItem item : payload.getSuggestions()) {
            if (item == null || item.getDomaineId() == null) {
                continue;
            }
            Domaine domaine = domaineById.get(item.getDomaineId());
            if (domaine == null) {
                continue;
            }
            suggestions.add(new AiDomainSuggestion(
                    domaine.getId(),
                    domaine.getName(),
                    item.getScore(),
                    item.getReason()
            ));
        }
        return suggestions.stream()
                .sorted(Comparator.comparing(AiDomainSuggestion::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<AiDomainSuggestion> fallbackDomaines(AiDomainSuggestionRequest request, List<Domaine> domaines) {
        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : 3;
        String missionText = buildMissionText(request.getTitle(), request.getDescription(),
                request.getRequirements(), request.getSkillsRequired()).toLowerCase(Locale.ROOT);
        if (missionText.isBlank()) {
            return List.of();
        }
        List<AiDomainSuggestion> suggestions = new ArrayList<>();
        for (Domaine domaine : domaines) {
            if (domaine == null || domaine.getName() == null) {
                continue;
            }
            double score = simpleScore(missionText, domaine.getName() + " " + safe(domaine.getDescription()));
            if (score > 0) {
                suggestions.add(new AiDomainSuggestion(domaine.getId(), domaine.getName(), score, "Keyword overlap"));
            }
        }
        return suggestions.stream()
                .sorted(Comparator.comparing(AiDomainSuggestion::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private AiSummaryResponse fallbackMissionSummary(String title, String description) {
        StringBuilder summary = new StringBuilder();
        if (title != null && !title.isBlank()) {
            summary.append(title.trim());
            if (description != null && !description.isBlank()) {
                summary.append(" - ");
            }
        }
        if (description != null && !description.isBlank()) {
            summary.append(trimToLength(description, 200));
        }
        List<String> steps = List.of("Review scope and budget", "Confirm timeline with client");
        return new AiSummaryResponse(summary.toString(), steps);
    }

    private AiSummaryResponse fallbackConversationSummary(List<String> messages) {
        String combined = messages == null ? "" : String.join(" ", messages);
        String summary = trimToLength(combined, 220);
        List<String> steps = List.of("Confirm next message or action", "Align on deliverables and timeline");
        return new AiSummaryResponse(summary.isBlank() ? "Conversation summary unavailable." : summary, steps);
    }

    private String buildDomainSuggestionPrompt(String missionText, List<Domaine> domaines, int limit, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        builder.append("Mission:\n").append(missionText).append("\n");
        builder.append("Available domains:\n");
        for (Domaine domaine : domaines) {
            if (domaine == null || domaine.getId() == null) {
                continue;
            }
            builder.append("- id=").append(domaine.getId())
                    .append(", name=\"").append(safe(domaine.getName())).append("\"")
                    .append(", description=\"").append(safe(domaine.getDescription())).append("\"\n");
        }
        builder.append("Return JSON: {\"suggestions\":[{\"domaineId\":1,\"score\":0.75,\"reason\":\"...\"}]}\n");
        builder.append("Return at most ").append(limit).append(" suggestions.");
        return builder.toString();
    }

    private String buildDraftPrompt(AiDraftRequest request, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        builder.append("Tone: ").append(safe(request.getTone())).append("\n");
        if (request.getMaxLength() != null) {
            builder.append("Max length: ").append(request.getMaxLength()).append(" characters\n");
        }
        builder.append("Title: ").append(safe(request.getTitle())).append("\n");
        builder.append("Description: ").append(safe(request.getDescription())).append("\n");
        builder.append("Requirements: ").append(safe(request.getRequirements())).append("\n");
        builder.append("Skills required: ").append(safe(request.getSkillsRequired())).append("\n");
        builder.append("Return JSON with keys: title, description, requirements, skillsSuggested, notes.");
        return builder.toString();
    }

    private String buildRewritePrompt(AiRewriteRequest request, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        builder.append("Intent: ").append(safe(request.getIntent())).append("\n");
        builder.append("Tone: ").append(safe(request.getTone())).append("\n");
        if (request.getMaxLength() != null) {
            builder.append("Max length: ").append(request.getMaxLength()).append(" characters\n");
        }
        builder.append("Content:\n").append(safe(request.getContent())).append("\n");
        builder.append("Return JSON with keys: content, notes.");
        return builder.toString();
    }

    private String buildMissionSummaryPrompt(String missionText, String status, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        builder.append("Mission status: ").append(safe(status)).append("\n");
        builder.append("Mission details:\n").append(missionText).append("\n");
        builder.append("Return JSON with keys: summary, nextSteps (array).");
        return builder.toString();
    }

    private String buildConversationSummaryPrompt(List<String> messages, String context, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        if (context != null && !context.isBlank()) {
            builder.append("Context: ").append(context).append("\n");
        }
        builder.append("Conversation messages:\n");
        int index = 1;
        for (String message : messages) {
            builder.append(index++).append(". ").append(safe(message)).append("\n");
        }
        builder.append("Return JSON with keys: summary, nextSteps (array).");
        return builder.toString();
    }

    private String buildModerationPrompt(String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("Classify the content as safe or unsafe. Use labels: OK, SPAM, HARASSMENT, HATE, SEXUAL, SCAM, VIOLENCE.\n");
        builder.append("Return JSON with keys: flagged (true/false), score (0-1), label, reason.\n");
        builder.append("Content:\n").append(safe(content));
        return builder.toString();
    }

    private String buildResumePrompt(String resumeText, String language) {
        StringBuilder builder = new StringBuilder();
        builder.append("Language: ").append(language).append("\n");
        builder.append("Extract skills with fields: name, level (BEGINNER|INTERMEDIATE|ADVANCED|EXPERT), yearsOfExperience, isCertified, certificationName.\n");
        builder.append("Return JSON with keys: summary, skills (array).\n");
        builder.append("Resume:\n").append(resumeText);
        return builder.toString();
    }

    private String normalizeLanguage(String language) {
        return (language == null || language.isBlank()) ? "fr" : language.trim();
    }

    private String truncate(String text) {
        return trimToLength(text, properties.getOpenai().getMaxInputChars());
    }

    private String trimToLength(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append("\n");
    }

    private double simpleScore(String missionText, String candidateText) {
        Set<String> tokens = tokenize(missionText);
        if (tokens.isEmpty()) {
            return 0.0;
        }
        String candidate = candidateText.toLowerCase(Locale.ROOT);
        long matches = tokens.stream().filter(candidate::contains).count();
        return matches / (double) tokens.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .stream()
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    @Data
    private static class DomainSuggestionPayload {
        private List<DomainSuggestionItem> suggestions;
    }

    @Data
    private static class DomainSuggestionItem {
        private Long domaineId;
        private Double score;
        private String reason;
    }

    @Data
    private static class DraftPayload {
        private String title;
        private String description;
        private String requirements;
        private String skillsSuggested;
        private String notes;
    }

    @Data
    private static class RewritePayload {
        private String content;
        private String notes;
    }

    @Data
    private static class SummaryPayload {
        private String summary;
        private List<String> nextSteps;
    }

    @Data
    private static class ModerationPayload {
        private Boolean flagged;
        private Double score;
        private String label;
        private String reason;
    }

    @Data
    private static class ResumePayload {
        private String summary;
        private List<AiSkillDto> skills;
    }
}
