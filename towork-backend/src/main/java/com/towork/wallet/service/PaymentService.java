package com.towork.wallet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import com.towork.wallet.entity.Transaction;
import com.towork.wallet.entity.TransactionStatus;
import com.towork.wallet.entity.Wallet;
import com.towork.wallet.entity.WalletTransaction;

public interface PaymentService {
    Transaction createTransaction(Transaction transaction);
    Transaction updateTransaction(Long id, Transaction transaction);
    void deleteTransaction(Long id);
    Transaction getTransactionById(Long id);
    Transaction getTransactionByReference(String transactionReference);
    List<Transaction> getTransactionsByClient(Long clientId);
    List<Transaction> getTransactionsByFreelancer(Long freelancerId);
    List<Transaction> getTransactionsByStatus(TransactionStatus status);
    List<Transaction> getTransactionsByMission(Long missionId);
    Page<Transaction> getAllTransactions(Pageable pageable);
    Transaction updateTransactionStatus(Long id, TransactionStatus status);
    Transaction processPayment(Long transactionId);
    Transaction refundTransaction(Long transactionId, String reason);
    Transaction payMilestone(Long milestoneId, Long clientId, Long freelancerId, String paymentMethod, String description);
    
    // Wallet operations
    Wallet createWallet(Wallet wallet);
    Wallet getWalletByClient(Long clientId);
    Wallet getWalletByFreelancer(Long freelancerId);
    Wallet updateWalletBalance(Long walletId, BigDecimal amount);
    Wallet addFunds(Long walletId, BigDecimal amount);
    Wallet withdrawFunds(Long walletId, BigDecimal amount);
    Wallet topUpClientWallet(Long clientId, BigDecimal amount);
    Wallet rechargeClientWallet(Long clientId, BigDecimal amount, String description);
    List<WalletTransaction> getWalletTransactionsByClient(Long clientId);
    List<WalletTransaction> getWalletTransactionsByFreelancer(Long freelancerId);
}
