-- Align wallet_transactions.type with com.towork.modules.paiement.WalletTransactionType.
ALTER TABLE wallet_transactions
DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;

ALTER TABLE wallet_transactions
ADD CONSTRAINT wallet_transactions_type_check
CHECK (type IN ('DEBIT', 'CREDIT', 'RECHARGE'));
