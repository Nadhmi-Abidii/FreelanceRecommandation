package com.towork.mission.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FreelancerMilestoneDto(
        Long id,
        String title,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        String status,
        Boolean isCompleted,
        LocalDateTime paidAt
) { }
