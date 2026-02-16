package com.towork.wallet.dto;

import com.towork.wallet.entity.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionDto(
        Long id,
        WalletTransactionType type,
        BigDecimal amount,
        String description,
        String missionTitle,
        String milestoneTitle,
        LocalDateTime createdAt
) {
}
