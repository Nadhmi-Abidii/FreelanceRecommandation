package com.towork.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.towork.milestone.entity.Milestone;

public interface MilestoneService {

    Milestone createMilestone(Milestone milestone);

    Milestone updateMilestone(Long id, Milestone milestone);

    void deleteMilestone(Long id);

    Milestone getMilestoneById(Long id);

    List<Milestone> getMilestonesByMission(Long missionId);

    List<Milestone> getMilestonesByMissionOrdered(Long missionId);

    List<Milestone> getCompletedMilestones(Long missionId);

    List<Milestone> getPendingMilestones(Long missionId);

    Page<Milestone> getAllMilestones(Pageable pageable);

    Milestone uploadDeliverable(Long milestoneId, MultipartFile file, String comment, UserDetails currentUser);

    Milestone acceptMilestone(Long milestoneId, String approvalNotes, UserDetails currentUser);

    Milestone rejectMilestone(Long milestoneId, String rejectionReason, UserDetails currentUser);

    Milestone deliverMilestone(Long id, String completionNotes);

    Milestone markMilestoneAsPending(Long id);

    Milestone validateMilestone(Long id, String approvalNotes);

    Milestone markMilestoneAsPaid(Long id);
}
