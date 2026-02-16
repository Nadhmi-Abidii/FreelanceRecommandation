package com.towork.wallet.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.milestone.entity.MilestoneStatus;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.towork.wallet.entity.Transaction;
import com.towork.wallet.entity.TransactionStatus;
import com.towork.wallet.entity.Wallet;
import com.towork.wallet.entity.WalletTransaction;
import com.towork.wallet.entity.WalletTransactionType;
import com.towork.wallet.repository.TransactionRepository;
import com.towork.wallet.repository.WalletRepository;
import com.towork.wallet.repository.WalletTransactionRepository;
import com.towork.wallet.service.PaymentService;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final MissionRepository missionRepository;
    private final MilestoneRepository milestoneRepository;

    @Override
    public Transaction createTransaction(Transaction transaction) {
        // Business Logic: Validate client and freelancer exist
        Client client = clientRepository.findById(transaction.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        
        Freelancer freelancer = freelancerRepository.findById(transaction.getFreelancer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        // Business Logic: Validate mission exists
        Mission mission = missionRepository.findById(transaction.getMission().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        // Business Logic: Validate milestone if provided
        if (transaction.getMilestone() != null) {
            Milestone milestone = milestoneRepository.findById(transaction.getMilestone().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
            
            // Business Logic: Validate milestone is completed
            if (!milestone.getIsCompleted()) {
                throw new BusinessException("Cannot create payment for incomplete milestone");
            }
        }

        // Business Logic: Validate amount is positive
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transaction amount must be positive");
        }

        // Business Logic: Generate unique transaction reference
        transaction.setTransactionReference(UUID.randomUUID().toString());
        transaction.setStatus(TransactionStatus.PENDING);
        
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction updateTransaction(Long id, Transaction transaction) {
        Transaction existingTransaction = getTransactionById(id);
        
        // Business Logic: Only allow updates if transaction is pending
        if (existingTransaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Cannot update a transaction that is not pending");
        }

        existingTransaction.setAmount(transaction.getAmount());
        existingTransaction.setDescription(transaction.getDescription());
        existingTransaction.setPaymentMethod(transaction.getPaymentMethod());
        existingTransaction.setPaymentGateway(transaction.getPaymentGateway());
        
        return transactionRepository.save(existingTransaction);
    }

    @Override
    public void deleteTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        
        // Business Logic: Only allow deletion if transaction is pending
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Cannot delete a transaction that is not pending");
        }

        transaction.setIsActive(false);
        transactionRepository.save(transaction);
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Override
    public Transaction getTransactionByReference(String transactionReference) {
        return transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + transactionReference));
    }

    @Override
    public List<Transaction> getTransactionsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        return transactionRepository.findByClient(client);
    }

    @Override
    public List<Transaction> getTransactionsByFreelancer(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
        return transactionRepository.findByFreelancer(freelancer);
    }

    @Override
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    @Override
    public List<Transaction> getTransactionsByMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        return transactionRepository.findByMission(mission);
    }

    @Override
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    @Override
    public Transaction updateTransactionStatus(Long id, TransactionStatus status) {
        Transaction transaction = getTransactionById(id);
        
        // Business Logic: Validate status transitions
        if (transaction.getStatus() == TransactionStatus.COMPLETED && status != TransactionStatus.COMPLETED) {
            throw new BusinessException("Cannot change status of a completed transaction");
        }
        
        if (transaction.getStatus() == TransactionStatus.REFUNDED && status != TransactionStatus.REFUNDED) {
            throw new BusinessException("Cannot change status of a refunded transaction");
        }

        transaction.setStatus(status);
        
        // Business Logic: If transaction is completed, update processed time
        if (status == TransactionStatus.COMPLETED) {
            transaction.setProcessedAt(LocalDateTime.now());
        }
        
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction processPayment(Long transactionId) {
        Transaction transaction = getTransactionById(transactionId);
        
        // Business Logic: Only process pending transactions
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Can only process pending transactions");
        }

        // Business Logic: Simulate payment processing
        // In a real implementation, this would integrate with payment gateways
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setGatewayTransactionId("GATEWAY_" + UUID.randomUUID().toString());
        
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction refundTransaction(Long transactionId, String reason) {
        Transaction transaction = getTransactionById(transactionId);
        
        // Business Logic: Only refund completed transactions
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException("Can only refund completed transactions");
        }

        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setFailureReason(reason);
        
        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public Transaction payMilestone(Long milestoneId, Long clientId, Long freelancerId, String paymentMethod, String description) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        if (milestone.getStatus() == MilestoneStatus.PAID) {
            throw new BusinessException("Milestone already paid");
        }
        if (milestone.getStatus() != MilestoneStatus.COMPLETED) {
            throw new BusinessException("Milestone must be completed before payment");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        Mission mission = missionRepository.findById(milestone.getMission().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        Wallet clientWallet = ensureClientWallet(client);
        Wallet freelancerWallet = ensureFreelancerWallet(freelancer);

        if (milestone.getAmount() == null) {
            throw new BusinessException("Milestone amount is required to process payment");
        }

        if (clientWallet.getBalance().compareTo(milestone.getAmount()) < 0) {
            throw new BusinessException("Insufficient funds in client wallet");
        }

        // Debit client wallet and credit freelancer wallet
        clientWallet.setBalance(clientWallet.getBalance().subtract(milestone.getAmount()));
        freelancerWallet.setBalance(freelancerWallet.getBalance().add(milestone.getAmount()));
        walletRepository.save(clientWallet);
        walletRepository.save(freelancerWallet);

        // Reflect the movement on each wallet: client is debited, freelancer is credited.
        recordWalletTransaction(clientWallet, WalletTransactionType.DEBIT, milestone.getAmount(),
                description, mission, milestone);
        recordWalletTransaction(freelancerWallet, WalletTransactionType.CREDIT, milestone.getAmount(),
                "Paiement recu pour " + milestone.getTitle(), mission, milestone);

        Transaction transaction = new Transaction();
        transaction.setAmount(milestone.getAmount());
        transaction.setClient(client);
        transaction.setFreelancer(freelancer);
        transaction.setMission(mission);
        transaction.setMilestone(milestone);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentGateway("WALLET");
        transaction.setTransactionReference(UUID.randomUUID().toString());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setDescription(description != null ? description : "Paiement du jalon " + milestone.getTitle());
        Transaction saved = transactionRepository.save(transaction);

        milestone.setStatus(MilestoneStatus.PAID);
        milestone.setIsCompleted(true);
        milestone.setPaidAt(LocalDateTime.now());
        milestoneRepository.save(milestone);

        return saved;
    }

    // Wallet operations
    @Override
    public Wallet createWallet(Wallet wallet) {
        // Business Logic: Validate that user doesn't already have a wallet
        if (wallet.getClient() != null) {
            if (walletRepository.findByClient(wallet.getClient()).isPresent()) {
                throw new BusinessException("Client already has a wallet");
            }
        }
        
        if (wallet.getFreelancer() != null) {
            if (walletRepository.findByFreelancer(wallet.getFreelancer()).isPresent()) {
                throw new BusinessException("Freelancer already has a wallet");
            }
        }

        wallet.setBalance(BigDecimal.ZERO);
        wallet.setIsActive(true);
        
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet getWalletByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        return walletRepository.findByClient(client)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for client"));
    }

    @Override
    public Wallet getWalletByFreelancer(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
        return walletRepository.findByFreelancer(freelancer)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for freelancer"));
    }

    @Override
    public Wallet updateWalletBalance(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
        
        wallet.setBalance(amount);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet addFunds(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }
        
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet withdrawFunds(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet topUpClientWallet(Long clientId, BigDecimal amount) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        Wallet wallet = ensureClientWallet(client);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet updated = walletRepository.save(wallet);
        recordWalletTransaction(updated, WalletTransactionType.CREDIT, amount, "Recharge du wallet client", null, null);
        return updated;
    }

    @Override
    public Wallet rechargeClientWallet(Long clientId, BigDecimal amount, String description) {
        return topUpClientWallet(clientId, amount);
    }

    @Override
    public List<WalletTransaction> getWalletTransactionsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        Wallet wallet = ensureClientWallet(client);
        return walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }

    @Override
    public List<WalletTransaction> getWalletTransactionsByFreelancer(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
        Wallet wallet = ensureFreelancerWallet(freelancer);
        return walletTransactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }

    private void recordWalletTransaction(Wallet wallet, WalletTransactionType type, BigDecimal amount,
                                         String description, Mission mission, Milestone milestone) {
        // Persisted enum names must be accepted by wallet_transactions_type_check or inserts (e.g. DEBIT) will fail.
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setRelatedMission(mission);
        tx.setRelatedMilestone(milestone);
        walletTransactionRepository.save(tx);
    }

    private Wallet ensureClientWallet(Client client) {
        return walletRepository.findActiveWalletByClient(client)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setClient(client);
                    wallet.setCurrency("EUR");
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setIsActive(true);
                    return walletRepository.save(wallet);
                });
    }

    private Wallet ensureFreelancerWallet(Freelancer freelancer) {
        return walletRepository.findActiveWalletByFreelancer(freelancer)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setFreelancer(freelancer);
                    wallet.setCurrency("EUR");
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setIsActive(true);
                    return walletRepository.save(wallet);
                });
    }
}

