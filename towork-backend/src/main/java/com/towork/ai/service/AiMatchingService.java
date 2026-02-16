package com.towork.ai.service;

import com.towork.ai.config.AiProperties;
import com.towork.ai.dto.AiFreelancerMatchDto;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.entity.Competence;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.CompetenceRepository;
import com.towork.user.repository.FreelancerRepository;
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
public class AiMatchingService {

    private final MissionRepository missionRepository;
    private final FreelancerRepository freelancerRepository;
    private final CompetenceRepository competenceRepository;
    private final CandidatureRepository candidatureRepository;
    private final AiTextService aiTextService;
    private final AiFeatureService aiFeatureService;
    private final AiProperties properties;

    public List<AiFreelancerMatchDto> recommendFreelancers(Long missionId, Integer limit) {
        int max = limit != null && limit > 0 ? limit : 5;
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));

        List<Freelancer> freelancers = freelancerRepository.findActiveAvailableFreelancers();
        if (freelancers == null || freelancers.isEmpty()) {
            freelancers = fallbackFreelancersFromCandidatures(mission);
        }
        if (freelancers == null || freelancers.isEmpty()) {
            return List.of();
        }

        String missionText = aiFeatureService.buildMissionText(
                mission.getTitle(), mission.getDescription(), mission.getRequirements(), mission.getSkillsRequired());
        if (missionText.isBlank()) {
            return List.of();
        }

        List<Candidate> candidates = buildCandidates(freelancers, missionText);
        candidates.sort(Comparator.comparing(Candidate::getHeuristicScore, Comparator.reverseOrder()));

        int shortlistSize = Math.min(20, candidates.size());
        List<Candidate> shortlist = candidates.subList(0, shortlistSize);

        if (!properties.isEnabled() || !properties.getFeatures().isMatching()) {
            return toFallbackMatches(shortlist, max);
        }

        String systemPrompt = "You rank freelancers for a mission. Return only JSON.";
        String userPrompt = buildMatchingPrompt(missionText, shortlist, max);

        try {
            MatchPayload payload = aiTextService.chatJson(systemPrompt, userPrompt, 0.2, 500, MatchPayload.class);
            List<AiFreelancerMatchDto> matches = normalizeMatches(payload, shortlist);
            if (matches.size() < max) {
                List<AiFreelancerMatchDto> fallback = toFallbackMatches(shortlist, max);
                for (AiFreelancerMatchDto fallbackMatch : fallback) {
                    boolean exists = matches.stream().anyMatch(m -> m.getFreelancerId().equals(fallbackMatch.getFreelancerId()));
                    if (!exists) {
                        matches.add(fallbackMatch);
                    }
                    if (matches.size() >= max) {
                        break;
                    }
                }
            }
            return matches.stream().limit(max).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("AI matching failed, using fallback: {}", ex.getMessage());
            return toFallbackMatches(shortlist, max);
        }
    }

    private List<Candidate> buildCandidates(List<Freelancer> freelancers, String missionText) {
        List<Candidate> candidates = new ArrayList<>();
        for (Freelancer freelancer : freelancers) {
            if (freelancer == null || freelancer.getId() == null) {
                continue;
            }
            String profile = buildFreelancerProfile(freelancer);
            double score = keywordScore(missionText, profile);
            candidates.add(new Candidate(freelancer, profile, score));
        }
        return candidates;
    }

    private List<Freelancer> fallbackFreelancersFromCandidatures(Mission mission) {
        List<Candidature> candidatures = candidatureRepository.findByMission(mission);
        if (candidatures == null || candidatures.isEmpty()) {
            return List.of();
        }
        return candidatures.stream()
                .map(Candidature::getFreelancer)
                .filter(Objects::nonNull)
                .filter(freelancer -> freelancer.getId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Freelancer::getId, freelancer -> freelancer, (a, b) -> a),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private String buildFreelancerProfile(Freelancer freelancer) {
        StringBuilder builder = new StringBuilder();
        append(builder, "Name", fullName(freelancer));
        append(builder, "Title", freelancer.getTitle());
        append(builder, "Bio", freelancer.getBio());
        append(builder, "Skills", freelancer.getSkills());

        List<Competence> competences = competenceRepository.findActiveByFreelancer(freelancer);
        if (competences != null && !competences.isEmpty()) {
            String names = competences.stream()
                    .map(Competence::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            append(builder, "Competences", names);
        }

        if (freelancer.getRating() != null) {
            append(builder, "Rating", freelancer.getRating().toPlainString());
        }
        return builder.toString();
    }

    private String buildMatchingPrompt(String missionText, List<Candidate> candidates, int limit) {
        StringBuilder builder = new StringBuilder();
        builder.append("Mission:\n").append(missionText).append("\n");
        builder.append("Candidates:\n");
        for (Candidate candidate : candidates) {
            Freelancer freelancer = candidate.getFreelancer();
            builder.append("- id=").append(freelancer.getId())
                    .append(", name=\"").append(fullName(freelancer)).append("\"")
                    .append(", title=\"").append(safe(freelancer.getTitle())).append("\"")
                    .append(", skills=\"").append(safe(freelancer.getSkills())).append("\"");
            if (freelancer.getRating() != null) {
                builder.append(", rating=").append(freelancer.getRating());
            }
            builder.append("\n");
        }
        builder.append("Return JSON: {\"matches\":[{\"freelancerId\":1,\"score\":0.8,\"reason\":\"...\"}]} ");
        builder.append("Return at most ").append(limit).append(" matches.");
        return builder.toString();
    }

    private List<AiFreelancerMatchDto> normalizeMatches(MatchPayload payload, List<Candidate> candidates) {
        if (payload == null || payload.getMatches() == null) {
            return List.of();
        }
        Map<Long, Candidate> candidateById = candidates.stream()
                .filter(c -> c.getFreelancer() != null && c.getFreelancer().getId() != null)
                .collect(Collectors.toMap(c -> c.getFreelancer().getId(), c -> c, (a, b) -> a));
        List<AiFreelancerMatchDto> matches = new ArrayList<>();
        for (MatchItem item : payload.getMatches()) {
            if (item == null || item.getFreelancerId() == null) {
                continue;
            }
            Candidate candidate = candidateById.get(item.getFreelancerId());
            if (candidate == null) {
                continue;
            }
            Freelancer freelancer = candidate.getFreelancer();
            matches.add(new AiFreelancerMatchDto(
                    freelancer.getId(),
                    fullName(freelancer),
                    freelancer.getTitle(),
                    item.getScore(),
                    item.getReason()
            ));
        }
        return matches;
    }

    private List<AiFreelancerMatchDto> toFallbackMatches(List<Candidate> candidates, int limit) {
        return candidates.stream()
                .sorted(Comparator.comparing(Candidate::getHeuristicScore, Comparator.reverseOrder()))
                .limit(limit)
                .map(candidate -> {
                    Freelancer freelancer = candidate.getFreelancer();
                    return new AiFreelancerMatchDto(
                            freelancer.getId(),
                            fullName(freelancer),
                            freelancer.getTitle(),
                            candidate.getHeuristicScore(),
                            "Keyword overlap"
                    );
                })
                .collect(Collectors.toList());
    }

    private double keywordScore(String missionText, String profileText) {
        Set<String> tokens = tokenize(missionText);
        if (tokens.isEmpty()) {
            return 0.0;
        }
        String profile = profileText == null ? "" : profileText.toLowerCase(Locale.ROOT);
        long matches = tokens.stream().filter(profile::contains).count();
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

    private void append(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append("\n");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String fullName(Freelancer freelancer) {
        String first = freelancer.getFirstName() == null ? "" : freelancer.getFirstName().trim();
        String last = freelancer.getLastName() == null ? "" : freelancer.getLastName().trim();
        String name = (first + " " + last).trim();
        return name.isBlank() ? "Freelancer " + freelancer.getId() : name;
    }

    @Data
    private static class Candidate {
        private final Freelancer freelancer;
        private final String profileText;
        private final double heuristicScore;
    }

    @Data
    private static class MatchPayload {
        private List<MatchItem> matches;
    }

    @Data
    private static class MatchItem {
        private Long freelancerId;
        private Double score;
        private String reason;
    }
}
