package com.towork.candidature.repository;

import com.towork.mission.entity.Mission;
import com.towork.user.entity.Freelancer;
import com.towork.user.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureStatus;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    List<Candidature> findByFreelancer(Freelancer freelancer);

    List<Candidature> findByFreelancerAndStatus(Freelancer freelancer, CandidatureStatus status);

    List<Candidature> findByMissionClientAndStatus(Client client, CandidatureStatus status);

    List<Candidature> findByMission(Mission mission);

    List<Candidature> findByStatus(CandidatureStatus status);
        // 👇 AJOUTER CETTE LIGNE
    void deleteAllByMission(Mission mission);

    @Query("SELECT c FROM Candidature c WHERE c.freelancer = :freelancer AND c.mission = :mission")
    Optional<Candidature> findByFreelancerAndMission(@Param("freelancer") Freelancer freelancer, @Param("mission") Mission mission);

    @Query("SELECT c FROM Candidature c WHERE c.mission = :mission AND c.status = :status")
    List<Candidature> findByMissionAndStatus(@Param("mission") Mission mission, @Param("status") CandidatureStatus status);

    @Query("SELECT COUNT(c) FROM Candidature c WHERE c.freelancer = :freelancer AND c.status = :status")
    Long countByFreelancerAndStatus(@Param("freelancer") Freelancer freelancer, @Param("status") CandidatureStatus status);
}
