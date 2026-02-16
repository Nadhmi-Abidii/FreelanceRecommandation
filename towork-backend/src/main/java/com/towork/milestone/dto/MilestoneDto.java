package com.towork.milestone.dto;

import com.towork.milestone.entity.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MilestoneDto(
        Long id,
        Long missionId,
        String missionTitle,
        String title,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        MilestoneStatus status,
        Boolean isCompleted,
        LocalDate completionDate,
        String completionNotes,
        String rejectionReason,
        LocalDateTime paidAt,
        String deliverableFileName,
        String deliverableOriginalName,
        String deliverableFileType,
        Long deliverableFileSize,
        LocalDateTime deliverableUploadedAt,
        String deliverableComment,
        Integer orderIndex,
        List<MilestoneDeliverableDto> deliverables
) {
}
