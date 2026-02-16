package com.towork.milestone.dto;

import com.towork.mission.entity.Mission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.towork.milestone.entity.MilestoneStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneResponse {
    private Long id;
    private Mission mission;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private MilestoneStatus status;
    private Boolean isCompleted;
    private LocalDate completionDate;
    private String completionNotes;
    private String rejectionReason;
    private LocalDateTime paidAt;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
