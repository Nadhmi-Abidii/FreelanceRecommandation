package com.towork.mission.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FreelancerMissionWithMilestonesDto(
        Long id,
        String title,
        String description,
        String status,
        BigDecimal totalAmount,
        String clientName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FreelancerMilestoneDto> milestones
) { }
