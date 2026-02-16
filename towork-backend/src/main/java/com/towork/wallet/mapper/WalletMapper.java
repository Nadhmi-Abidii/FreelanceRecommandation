package com.towork.wallet.mapper;

import com.towork.wallet.entity.Wallet;
import com.towork.wallet.entity.Transaction;

import java.util.Collections;
import java.util.List;
import com.towork.wallet.dto.WalletSnapshotDto;
import com.towork.wallet.dto.WalletTransactionDto;
import com.towork.wallet.entity.WalletTransaction;

public final class WalletMapper {
    private WalletMapper() {
    }

    public static WalletSnapshotDto toSnapshot(Wallet wallet, List<WalletTransaction> transactions) {
        List<WalletTransaction> txs = transactions != null ? transactions : Collections.emptyList();
        return new WalletSnapshotDto(
                wallet != null ? wallet.getId() : null,
                wallet != null ? wallet.getBalance() : null,
                wallet != null ? wallet.getCurrency() : null,
                txs.stream().map(WalletMapper::toDto).toList()
        );
    }

    public static WalletTransactionDto toDto(WalletTransaction tx) {
        if (tx == null) {
            return null;
        }
        String missionTitle = tx.getRelatedMission() != null ? tx.getRelatedMission().getTitle() : null;
        String milestoneTitle = tx.getRelatedMilestone() != null ? tx.getRelatedMilestone().getTitle() : null;
        return new WalletTransactionDto(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getDescription(),
                missionTitle,
                milestoneTitle,
                tx.getCreatedAt()
        );
    }
}
