package com.towork.milestone.dto;

import java.time.LocalDateTime;

public record MilestoneDeliverableDto(
        Long id,
        String fileName,
        String downloadUrl,
        String comment,
        String contentType,
        String uploadedBy,
        LocalDateTime createdAt
) {
}
