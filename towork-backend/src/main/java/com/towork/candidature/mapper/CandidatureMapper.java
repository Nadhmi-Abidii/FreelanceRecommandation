package com.towork.candidature.mapper;

import com.towork.user.entity.Freelancer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.towork.candidature.dto.CandidatureMessageResponse;
import com.towork.candidature.dto.CandidatureResponse;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;

public final class CandidatureMapper {
    private CandidatureMapper() {}

    public static CandidatureResponse toDto(Candidature candidature) {
        if (candidature == null) {
            return null;
        }

        Freelancer freelancer = candidature.getFreelancer();
        CandidatureResponse.FreelancerSummary summary = null;
        if (freelancer != null) {
            summary = CandidatureResponse.FreelancerSummary.builder()
                    .id(freelancer.getId())
                    .firstName(freelancer.getFirstName())
                    .lastName(freelancer.getLastName())
                    .title(freelancer.getTitle())
                    .email(freelancer.getEmail())
                    .phone(freelancer.getPhone())
                    .city(freelancer.getCity())
                    .country(freelancer.getCountry())
                    .build();
        }

        List<CandidatureMessageResponse> messages = candidature.getMessages() == null ? List.of()
                : candidature.getMessages().stream()
                .sorted(Comparator.comparing(CandidatureMessage::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CandidatureMapper::toMessageDto)
                .collect(Collectors.toList());

        return CandidatureResponse.builder()
                .id(candidature.getId())
                .missionId(candidature.getMission() != null ? candidature.getMission().getId() : null)
                .freelancer(summary)
                .coverLetter(candidature.getCoverLetter())
                .resumeUrl(candidature.getResumeUrl())
                .proposedPrice(candidature.getProposedPrice())
                .proposedDuration(candidature.getProposedDuration())
                .status(candidature.getStatus())
                .clientMessage(candidature.getClientMessage())
                .createdAt(candidature.getCreatedAt())
                .updatedAt(candidature.getUpdatedAt())
                .messages(messages)
                .build();
    }

    public static CandidatureMessageResponse toMessageDto(CandidatureMessage message) {
        if (message == null) return null;
        return CandidatureMessageResponse.builder()
                .id(message.getId())
                .author(message.getAuthor())
                .content(message.getContent())
                .resumeUrl(message.getResumeUrl())
                .isFlagged(message.getIsFlagged())
                .flagScore(message.getFlagScore())
                .flagLabel(message.getFlagLabel())
                .flagReason(message.getFlagReason())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
