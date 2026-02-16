package com.towork.candidature.dto;

import lombok.Data;

@Data
public class CreateCandidatureRequest {
    private Long freelancerId;
    private Long missionId;
    private Double proposedPrice;
    private Integer proposedDuration;
    private String coverLetter;
    private String resumeUrl;
}
