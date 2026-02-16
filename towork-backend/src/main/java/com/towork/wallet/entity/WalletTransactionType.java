package com.towork.wallet.entity;

/**
 * Enum values are persisted as plain strings in wallet_transactions.type and must
 * stay aligned with the PostgreSQL CHECK constraint wallet_transactions_type_check
 * (otherwise inserts like DEBIT will fail with a constraint violation).
 */
public enum WalletTransactionType {
    /**
     * Money entering a wallet (e.g. freelancer receives a milestone payout).
     */
    CREDIT,

    /**
     * Money leaving a wallet (e.g. client pays for a milestone).
     */
    DEBIT,

    /**
     * Kept for compatibility with existing data for explicit wallet recharges.
     */
    RECHARGE
}
