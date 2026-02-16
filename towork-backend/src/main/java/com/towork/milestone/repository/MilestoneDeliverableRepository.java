package com.towork.milestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;

@Repository
public interface MilestoneDeliverableRepository extends JpaRepository<MilestoneDeliverable, Long> {
    List<MilestoneDeliverable> findByMilestoneOrderByCreatedAtDesc(Milestone milestone);
}
