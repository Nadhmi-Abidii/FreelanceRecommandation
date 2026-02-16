package com.towork.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaidMilestoneDto(
        Long milestoneId,
        String missionTitle,
        String milestoneTitle,
        BigDecimal amount,
        LocalDateTime paidAt
) {
}
