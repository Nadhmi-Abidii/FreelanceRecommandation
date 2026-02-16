package com.towork.wallet.repository;

import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.towork.wallet.entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByClient(Client client);

    Optional<Wallet> findByFreelancer(Freelancer freelancer);

    @Query("SELECT w FROM Wallet w WHERE w.client = :client AND w.isActive = true")
    Optional<Wallet> findActiveWalletByClient(@Param("client") Client client);

    @Query("SELECT w FROM Wallet w WHERE w.freelancer = :freelancer AND w.isActive = true")
    Optional<Wallet> findActiveWalletByFreelancer(@Param("freelancer") Freelancer freelancer);
}
