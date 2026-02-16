package com.towork.candidature.dto;

import com.towork.candidature.entity.CandidatureStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CandidatureResponse {
    Long id;
    Long missionId;
    FreelancerSummary freelancer;
    String coverLetter;
    String resumeUrl;
    Double proposedPrice;
    Integer proposedDuration;
    CandidatureStatus status;
    String clientMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    java.util.List<CandidatureMessageResponse> messages;

    @Value
    @Builder
    public static class FreelancerSummary {
        Long id;
        String firstName;
        String lastName;
        String title;
        String email;
        String phone;
        String city;
        String country;
    }
}
