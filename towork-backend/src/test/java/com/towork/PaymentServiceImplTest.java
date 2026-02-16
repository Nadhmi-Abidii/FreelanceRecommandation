package com.towork;


import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.wallet.service.impl.PaymentServiceImpl;
import com.towork.wallet.entity.Transaction;
import com.towork.wallet.repository.TransactionRepository;
import com.towork.wallet.entity.TransactionStatus;
import com.towork.wallet.entity.Wallet;
import com.towork.wallet.repository.WalletRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private FreelancerRepository freelancerRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private MilestoneRepository milestoneRepository;

    @InjectMocks
    private PaymentServiceImpl service;

    private Client client;
    private Freelancer freelancer;
    private Mission mission;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);

        freelancer = new Freelancer();
        freelancer.setId(2L);

        mission = new Mission();
        mission.setId(3L);
    }

    private Transaction draftTransaction(BigDecimal amount) {
        Transaction t = new Transaction();
        t.setClient(client);
        t.setFreelancer(freelancer);
        t.setMission(mission);
        t.setAmount(amount);
        return t;
    }

    @Nested
    class CreateTransaction {

        @Test
        @DisplayName("OK: crée transaction PENDING avec références générées")
        void create_ok() {
            Milestone milestone = new Milestone();
            milestone.setId(5L);
            milestone.setIsCompleted(true);

            Transaction transaction = draftTransaction(new BigDecimal("150.00"));
            transaction.setMilestone(milestone);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(freelancerRepository.findById(2L)).thenReturn(Optional.of(freelancer));
            when(missionRepository.findById(3L)).thenReturn(Optional.of(mission));
            when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            Transaction saved = service.createTransaction(transaction);

            assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(saved.getTransactionReference()).isNotBlank();
            verify(transactionRepository).save(saved);
        }

        @Test
        @DisplayName("KO: milestone non complétée -> BusinessException")
        void create_incompleteMilestone() {
            Milestone milestone = new Milestone();
            milestone.setId(5L);
            milestone.setIsCompleted(false);

            Transaction transaction = draftTransaction(new BigDecimal("150.00"));
            transaction.setMilestone(milestone);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(freelancerRepository.findById(2L)).thenReturn(Optional.of(freelancer));
            when(missionRepository.findById(3L)).thenReturn(Optional.of(mission));
            when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

            assertThatThrownBy(() -> service.createTransaction(transaction))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("incomplete milestone");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("KO: montant négatif -> BusinessException")
        void create_negativeAmount() {
            Transaction transaction = draftTransaction(new BigDecimal("-10.00"));

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(freelancerRepository.findById(2L)).thenReturn(Optional.of(freelancer));
            when(missionRepository.findById(3L)).thenReturn(Optional.of(mission));

            assertThatThrownBy(() -> service.createTransaction(transaction))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("amount must be positive");

            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    class StatusUpdates {

        @Test
        @DisplayName("OK: passage à COMPLETED renseigne processedAt")
        void updateStatus_completed() {
            Transaction existing = draftTransaction(new BigDecimal("80.00"));
            existing.setId(10L);
            existing.setStatus(TransactionStatus.PENDING);

            when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            Transaction updated = service.updateTransactionStatus(10L, TransactionStatus.COMPLETED);

            assertThat(updated.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(updated.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("KO: processPayment refuse status != PENDING")
        void processPayment_notPending() {
            Transaction t = draftTransaction(new BigDecimal("40.00"));
            t.setId(9L);
            t.setStatus(TransactionStatus.COMPLETED);

            when(transactionRepository.findById(9L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.processPayment(9L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pending transactions");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("KO: refund nécessite status COMPLETED")
        void refund_notCompleted() {
            Transaction t = draftTransaction(new BigDecimal("120.00"));
            t.setId(8L);
            t.setStatus(TransactionStatus.PENDING);

            when(transactionRepository.findById(8L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.refundTransaction(8L, "client request"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("refund completed");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("KO: transaction inconnue -> ResourceNotFoundException")
        void updateStatus_notFound() {
            when(transactionRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTransactionStatus(404L, TransactionStatus.COMPLETED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("404");
        }
    }

    @Nested
    class Wallets {

        @Test
        @DisplayName("OK: addFunds cumule le solde")
        void addFunds_ok() {
            Wallet wallet = new Wallet();
            wallet.setId(11L);
            wallet.setBalance(new BigDecimal("100.00"));

            when(walletRepository.findById(11L)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet updated = service.addFunds(11L, new BigDecimal("25.50"));

            assertThat(updated.getBalance()).isEqualByComparingTo("125.50");
        }

        @Test
        @DisplayName("KO: withdrawFunds refuse fonds insuffisants")
        void withdraw_insufficient() {
            Wallet wallet = new Wallet();
            wallet.setId(12L);
            wallet.setBalance(new BigDecimal("30.00"));

            when(walletRepository.findById(12L)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> service.withdrawFunds(12L, new BigDecimal("50.00")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Insufficient funds");
        }
    }
}