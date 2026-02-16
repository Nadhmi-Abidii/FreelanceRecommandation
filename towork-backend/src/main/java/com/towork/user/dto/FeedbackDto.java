package com.towork.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;
    private Long clientId;
    private Long freelancerId;
    private Long missionId;
    private String clientName;
    private String freelancerName;
    private String missionTitle;
    private BigDecimal rating;
    private String comment;
    private BigDecimal communicationRating;
    private BigDecimal qualityRating;
    private BigDecimal timelinessRating;
    private Boolean isPublic;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
