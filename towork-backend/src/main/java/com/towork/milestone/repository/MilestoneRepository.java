package com.towork.milestone.repository;

import com.towork.mission.entity.Mission;
import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneStatus;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByMissionAndIsActiveTrue(Mission mission);

    List<Milestone> findByMissionAndIsActiveTrueOrderByOrderIndexAsc(Mission mission);

    List<Milestone> findByIsCompletedAndIsActiveTrue(Boolean isCompleted);

    void deleteAllByMission(Mission mission);

    @Query("SELECT m FROM Milestone m WHERE m.mission = :mission AND m.isCompleted = :isCompleted AND m.isActive = true")
    List<Milestone> findByMissionAndIsCompletedAndIsActiveTrue(@Param("mission") Mission mission,
            @Param("isCompleted") Boolean isCompleted);

    @Query("SELECT COUNT(m) FROM Milestone m WHERE m.mission = :mission AND m.isActive = true")
    Long countByMission(@Param("mission") Mission mission);

    @Query("SELECT COUNT(m) FROM Milestone m WHERE m.mission = :mission AND m.isCompleted = true AND m.isActive = true")
    Long countCompletedByMission(@Param("mission") Mission mission);

    @Query("SELECT m FROM Milestone m WHERE m.mission.assignedFreelancer = :freelancer AND m.status = com.towork.milestone.entity.MilestoneStatus.PAID AND m.isActive = true ORDER BY m.paidAt DESC")
    List<Milestone> findPaidByFreelancer(@Param("freelancer") Freelancer freelancer);
}
