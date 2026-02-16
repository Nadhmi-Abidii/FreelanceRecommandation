package com.towork.wallet.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletSnapshotDto(
        Long walletId,
        BigDecimal balance,
        String currency,
        List<WalletTransactionDto> transactions
) {
}
