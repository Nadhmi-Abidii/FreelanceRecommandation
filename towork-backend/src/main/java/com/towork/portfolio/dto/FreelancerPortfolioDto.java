package com.towork.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record FreelancerPortfolioDto(
        BigDecimal totalAmountEarned,
        List<PaidMilestoneDto> paidMilestones
) {
}
