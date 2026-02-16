package com.towork.wallet.repository;

import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.towork.wallet.entity.Transaction;
import com.towork.wallet.entity.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByClient(Client client);

    List<Transaction> findByFreelancer(Freelancer freelancer);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByMission(Mission mission);

    @Query("SELECT t FROM Transaction t WHERE t.client = :client AND t.status = :status")
    List<Transaction> findByClientAndStatus(@Param("client") Client client, @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.freelancer = :freelancer AND t.status = :status")
    List<Transaction> findByFreelancerAndStatus(@Param("freelancer") Freelancer freelancer, @Param("status") TransactionStatus status);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByGatewayTransactionId(String gatewayTransactionId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.client = :client AND t.status = :status")
    Long countByClientAndStatus(@Param("client") Client client, @Param("status") TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.freelancer = :freelancer AND t.status = :status")
    Long countByFreelancerAndStatus(@Param("freelancer") Freelancer freelancer, @Param("status") TransactionStatus status);
}
