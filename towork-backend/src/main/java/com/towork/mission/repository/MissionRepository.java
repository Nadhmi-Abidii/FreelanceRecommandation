package com.towork.mission.repository;

import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.towork.mission.entity.BudgetType;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByClient(Client client);

    List<Mission> findByDomaine(Domaine domaine);

    List<Mission> findByStatus(MissionStatus status);

    List<Mission> findByTypeTravail(TypeTravail typeTravail);

    List<Mission> findByNiveauExperience(NiveauExperience niveauExperience);

    List<Mission> findByBudgetType(BudgetType budgetType);

    @Query("SELECT m FROM Mission m WHERE m.client = :client AND m.status = :status")
    List<Mission> findByClientAndStatus(@Param("client") Client client, @Param("status") MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.domaine = :domaine AND m.status = :status")
    List<Mission> findByDomaineAndStatus(@Param("domaine") Domaine domaine, @Param("status") MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.status = :status AND m.isUrgent = true")
    List<Mission> findUrgentMissionsByStatus(@Param("status") MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.status = 'PUBLISHED' ORDER BY m.createdAt DESC")
    List<Mission> findPublishedMissionsOrderByCreatedAt();

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.client = :client AND m.status = :status")
    Long countByClientAndStatus(@Param("client") Client client, @Param("status") MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.assignedFreelancer = :freelancer AND m.isActive = true AND m.status != com.towork.mission.entity.MissionStatus.CANCELLED")
    List<Mission> findActiveByFreelancer(@Param("freelancer") Freelancer freelancer);
}
