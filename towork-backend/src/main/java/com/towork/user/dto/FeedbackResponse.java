package com.towork.user.dto;

import com.towork.user.entity.FeedbackDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private Long missionId;
    private Long authorUserId;
    private Long targetUserId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private FeedbackDirection direction;
}
