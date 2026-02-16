package com.towork.wallet.controller;

import com.towork.config.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.wallet.dto.MilestonePaymentRequest;
import com.towork.wallet.entity.Transaction;
import com.towork.wallet.entity.TransactionStatus;
import com.towork.wallet.entity.Wallet;
import com.towork.wallet.service.PaymentService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaiementController {

    private final PaymentService paymentService;

    @PostMapping("/transactions")
    public ResponseEntity<MessageResponse> createTransaction(@RequestBody Transaction transaction) {
        Transaction createdTransaction = paymentService.createTransaction(transaction);
        return ResponseEntity.ok(MessageResponse.success("Transaction created successfully", createdTransaction));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<MessageResponse> getTransactionById(@PathVariable Long id) {
        Transaction transaction = paymentService.getTransactionById(id);
        return ResponseEntity.ok(MessageResponse.success("Transaction retrieved successfully", transaction));
    }

    @GetMapping("/transactions")
    public ResponseEntity<MessageResponse> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = paymentService.getAllTransactions(pageable);
        return ResponseEntity.ok(MessageResponse.success("Transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/client/{clientId}")
    public ResponseEntity<MessageResponse> getTransactionsByClient(@PathVariable Long clientId) {
        List<Transaction> transactions = paymentService.getTransactionsByClient(clientId);
        return ResponseEntity.ok(MessageResponse.success("Transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/freelancer/{freelancerId}")
    public ResponseEntity<MessageResponse> getTransactionsByFreelancer(@PathVariable Long freelancerId) {
        List<Transaction> transactions = paymentService.getTransactionsByFreelancer(freelancerId);
        return ResponseEntity.ok(MessageResponse.success("Transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/status/{status}")
    public ResponseEntity<MessageResponse> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        List<Transaction> transactions = paymentService.getTransactionsByStatus(status);
        return ResponseEntity.ok(MessageResponse.success("Transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/mission/{missionId}")
    public ResponseEntity<MessageResponse> getTransactionsByMission(@PathVariable Long missionId) {
        List<Transaction> transactions = paymentService.getTransactionsByMission(missionId);
        return ResponseEntity.ok(MessageResponse.success("Transactions retrieved successfully", transactions));
    }

    @PutMapping("/transactions/{id}/status")
    public ResponseEntity<MessageResponse> updateTransactionStatus(@PathVariable Long id, @RequestParam TransactionStatus status) {
        Transaction transaction = paymentService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(MessageResponse.success("Transaction status updated successfully", transaction));
    }

    @PostMapping("/transactions/{id}/process")
    public ResponseEntity<MessageResponse> processPayment(@PathVariable Long id) {
        Transaction transaction = paymentService.processPayment(id);
        return ResponseEntity.ok(MessageResponse.success("Payment processed successfully", transaction));
    }

    @PostMapping("/transactions/{id}/refund")
    public ResponseEntity<MessageResponse> refundTransaction(@PathVariable Long id, @RequestParam String reason) {
        Transaction transaction = paymentService.refundTransaction(id, reason);
        return ResponseEntity.ok(MessageResponse.success("Transaction refunded successfully", transaction));
    }

    @PostMapping("/milestones/{id}/pay")
    public ResponseEntity<MessageResponse> payMilestone(
            @PathVariable Long id,
            @RequestBody MilestonePaymentRequest request) {
        Transaction transaction = paymentService.payMilestone(
                id,
                request.getClientId(),
                request.getFreelancerId(),
                request.getPaymentMethod(),
                request.getDescription()
        );
        return ResponseEntity.ok(MessageResponse.success("Milestone paid successfully", transaction));
    }

    // Wallet endpoints
    @PostMapping("/wallets")
    public ResponseEntity<MessageResponse> createWallet(@RequestBody Wallet wallet) {
        Wallet createdWallet = paymentService.createWallet(wallet);
        return ResponseEntity.ok(MessageResponse.success("Wallet created successfully", createdWallet));
    }

    @GetMapping("/wallets/client/{clientId}")
    public ResponseEntity<MessageResponse> getWalletByClient(@PathVariable Long clientId) {
        Wallet wallet = paymentService.getWalletByClient(clientId);
        return ResponseEntity.ok(MessageResponse.success("Wallet retrieved successfully", wallet));
    }

    @GetMapping("/wallets/freelancer/{freelancerId}")
    public ResponseEntity<MessageResponse> getWalletByFreelancer(@PathVariable Long freelancerId) {
        Wallet wallet = paymentService.getWalletByFreelancer(freelancerId);
        return ResponseEntity.ok(MessageResponse.success("Wallet retrieved successfully", wallet));
    }

    @PutMapping("/wallets/{id}/balance")
    public ResponseEntity<MessageResponse> updateWalletBalance(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Wallet wallet = paymentService.updateWalletBalance(id, amount);
        return ResponseEntity.ok(MessageResponse.success("Wallet balance updated successfully", wallet));
    }

    @PostMapping("/wallets/{id}/add-funds")
    public ResponseEntity<MessageResponse> addFunds(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Wallet wallet = paymentService.addFunds(id, amount);
        return ResponseEntity.ok(MessageResponse.success("Funds added successfully", wallet));
    }

    @PostMapping("/wallets/{id}/withdraw")
    public ResponseEntity<MessageResponse> withdrawFunds(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Wallet wallet = paymentService.withdrawFunds(id, amount);
        return ResponseEntity.ok(MessageResponse.success("Funds withdrawn successfully", wallet));
    }

    @PostMapping("/wallets/client/{clientId}/topup")
    public ResponseEntity<MessageResponse> topUpClientWallet(
            @PathVariable Long clientId,
            @RequestParam BigDecimal amount) {
        Wallet wallet = paymentService.topUpClientWallet(clientId, amount);
        return ResponseEntity.ok(MessageResponse.success("Wallet credited successfully", wallet));
    }
}
