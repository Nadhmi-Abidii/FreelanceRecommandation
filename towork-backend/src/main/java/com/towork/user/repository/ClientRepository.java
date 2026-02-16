package com.towork.user.repository;

import com.towork.user.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    List<Client> findByIsVerified(Boolean isVerified);

    List<Client> findByCompanyNameContainingIgnoreCase(String companyName);

    List<Client> findByIndustry(String industry);

    List<Client> findByCity(String city);

    List<Client> findByCountry(String country);

    @Query("SELECT c FROM Client c WHERE c.isActive = true AND c.isVerified = true")
    List<Client> findActiveVerifiedClients();

    @Query("SELECT COUNT(c) FROM Client c WHERE c.isVerified = true")
    Long countVerifiedClients();

    @Query("SELECT c FROM Client c WHERE c.email = :email AND c.isActive = true")
    Optional<Client> findActiveByEmail(@Param("email") String email);
}
