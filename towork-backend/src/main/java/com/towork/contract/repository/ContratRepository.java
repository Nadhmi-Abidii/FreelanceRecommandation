package com.towork.contract.repository;

import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.towork.contract.entity.Contrat;
import com.towork.contract.entity.EtatContrat;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, Long> {

    List<Contrat> findByClient(Client client);

    List<Contrat> findByFreelancer(Freelancer freelancer);

    List<Contrat> findByEtat(EtatContrat etat);

    @Query("SELECT c FROM Contrat c WHERE c.client = :client AND c.etat = :etat")
    List<Contrat> findByClientAndEtat(@Param("client") Client client, @Param("etat") EtatContrat etat);

    @Query("SELECT c FROM Contrat c WHERE c.freelancer = :freelancer AND c.etat = :etat")
    List<Contrat> findByFreelancerAndEtat(@Param("freelancer") Freelancer freelancer, @Param("etat") EtatContrat etat);

    @Query("SELECT c FROM Contrat c WHERE c.client = :client AND c.freelancer = :freelancer")
    List<Contrat> findByClientAndFreelancer(@Param("client") Client client, @Param("freelancer") Freelancer freelancer);

    @Query("SELECT COUNT(c) FROM Contrat c WHERE c.client = :client AND c.etat = :etat")
    Long countByClientAndEtat(@Param("client") Client client, @Param("etat") EtatContrat etat);

    @Query("SELECT COUNT(c) FROM Contrat c WHERE c.freelancer = :freelancer AND c.etat = :etat")
    Long countByFreelancerAndEtat(@Param("freelancer") Freelancer freelancer, @Param("etat") EtatContrat etat);
}
