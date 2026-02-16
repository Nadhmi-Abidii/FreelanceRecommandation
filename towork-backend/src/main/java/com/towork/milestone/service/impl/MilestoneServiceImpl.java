package com.towork.milestone.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ForbiddenActionException;
import com.towork.exception.InvalidMilestoneStatusException;
import com.towork.exception.MilestoneNotFoundException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.file.FileStorageService;
import com.towork.file.FileUtils;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.wallet.service.PaymentService;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;
import com.towork.milestone.entity.MilestoneStatus;
import com.towork.milestone.repository.MilestoneDeliverableRepository;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.milestone.service.MilestoneService;

@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneServiceImpl implements MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final MissionRepository missionRepository;
    private final PaymentService paymentService;
    private final MilestoneDeliverableRepository deliverableRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Milestone createMilestone(Milestone milestone) {
        if (milestone.getMission() == null || milestone.getMission().getId() == null) {
            throw new BusinessException("Mission id is required to create a milestone");
        }

        Mission mission = missionRepository.findById(milestone.getMission().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        if (!Boolean.TRUE.equals(mission.getIsActive())) {
            throw new BusinessException("Cannot create milestone for an inactive mission");
        }

        validateAmountAndDueDate(milestone.getAmount(), milestone.getDueDate());

        milestone.setMission(mission);
        milestone.setStatus(MilestoneStatus.DRAFT);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(null);
        milestone.setRejectionReason(null);
        milestone.setPaidAt(null);
        milestone.setIsActive(true);
        if (milestone.getOrderIndex() == null || milestone.getOrderIndex() <= 0) {
            Long existing = milestoneRepository.countByMission(mission);
            long nextIndex = (existing == null ? 0 : existing) + 1;
            milestone.setOrderIndex((int) nextIndex);
        }

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone updateMilestone(Long id, Milestone milestone) {
        Milestone existing = getMilestoneById(id);

        if (existing.getStatus() == MilestoneStatus.COMPLETED || existing.getStatus() == MilestoneStatus.PAID) {
            throw new BusinessException("Cannot update a completed or paid milestone");
        }

        validateAmountAndDueDate(milestone.getAmount(), milestone.getDueDate());

        existing.setTitle(milestone.getTitle());
        existing.setDescription(milestone.getDescription());
        existing.setAmount(milestone.getAmount());
        existing.setDueDate(milestone.getDueDate());
        existing.setOrderIndex(milestone.getOrderIndex());

        return milestoneRepository.save(existing);
    }

    @Override
    public void deleteMilestone(Long id) {
        Milestone milestone = getMilestoneById(id);

        if (milestone.getStatus() == MilestoneStatus.PAID) {
            throw new BusinessException("Cannot delete a paid milestone");
        }

        milestone.setIsActive(false);
        milestoneRepository.save(milestone);
    }

    @Override
    @Transactional(readOnly = true)
    public Milestone getMilestoneById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new MilestoneNotFoundException("Milestone not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Milestone> getMilestonesByMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        return milestoneRepository.findByMissionAndIsActiveTrue(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Milestone> getMilestonesByMissionOrdered(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        return milestoneRepository.findByMissionAndIsActiveTrueOrderByOrderIndexAsc(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Milestone> getCompletedMilestones(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        return milestoneRepository.findByMissionAndIsCompletedAndIsActiveTrue(mission, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Milestone> getPendingMilestones(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
        return milestoneRepository.findByMissionAndIsCompletedAndIsActiveTrue(mission, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Milestone> getAllMilestones(Pageable pageable) {
        return milestoneRepository.findAll(pageable);
    }

    @Override
    public Milestone uploadDeliverable(Long milestoneId, MultipartFile file, String comment, UserDetails currentUser) {
        Milestone milestone = getMilestoneById(milestoneId);
        ensureFreelancerAccess(milestone, currentUser);

        if (milestone.getStatus() == MilestoneStatus.PAID) {
            throw new InvalidMilestoneStatusException("Cannot upload deliverable for a paid milestone");
        }
        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new InvalidMilestoneStatusException("Milestone is already completed");
        }
        if (milestone.getStatus() == MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStatusException("Milestone already submitted and pending review");
        }

        Freelancer uploader = requireAssignedFreelancer(milestone.getMission());
        validateFile(file);

        String storageKey;
        try {
            storageKey = fileStorageService.storeFile(file);
        } catch (Exception e) {
            throw new BusinessException("Unable to store deliverable " + file.getOriginalFilename(), e);
        }

        MilestoneDeliverable deliverable = new MilestoneDeliverable();
        deliverable.setMilestone(milestone);
        deliverable.setUploader(uploader);
        deliverable.setFileName(file.getOriginalFilename());
        deliverable.setStorageKey(storageKey);
        deliverable.setContentType(file.getContentType());
        deliverable.setComment(comment);
        deliverableRepository.save(deliverable);

        milestone.setDeliverableFileName(storageKey);
        milestone.setDeliverableOriginalName(file.getOriginalFilename());
        milestone.setDeliverableFileType(FileUtils.getFileType(file.getOriginalFilename()));
        milestone.setDeliverableFileSize(file.getSize());
        milestone.setDeliverableUploadedAt(LocalDateTime.now());
        milestone.setDeliverableComment(comment);
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(null);
        milestone.setRejectionReason(null);

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone acceptMilestone(Long milestoneId, String approvalNotes, UserDetails currentUser) {
        Milestone milestone = getMilestoneById(milestoneId);
        ensureClientAccess(milestone, currentUser);
        return finalizeAcceptance(milestone, approvalNotes);
    }

    @Override
    public Milestone rejectMilestone(Long milestoneId, String rejectionReason, UserDetails currentUser) {
        Milestone milestone = getMilestoneById(milestoneId);
        ensureClientAccess(milestone, currentUser);

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStatusException("Only submitted milestones can be rejected");
        }

        milestone.setStatus(MilestoneStatus.REJECTED);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(null);
        milestone.setRejectionReason(rejectionReason);
        milestone.setPaidAt(null);

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone deliverMilestone(Long id, String completionNotes) {
        Milestone milestone = getMilestoneById(id);

        if (!Boolean.TRUE.equals(milestone.getIsActive())) {
            throw new BusinessException("Milestone is inactive");
        }
        if (milestone.getStatus() == MilestoneStatus.PAID || milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new InvalidMilestoneStatusException("Cannot submit a completed or paid milestone");
        }
        if (milestone.getStatus() != MilestoneStatus.DRAFT && milestone.getStatus() != MilestoneStatus.IN_PROGRESS
                && milestone.getStatus() != MilestoneStatus.REJECTED) {
            throw new InvalidMilestoneStatusException("Only milestones in progress can be submitted");
        }

        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(completionNotes);
        milestone.setRejectionReason(null);

        return milestoneRepository.save(milestone);
    }

    @Override
    public Milestone markMilestoneAsPending(Long id) {
        Milestone milestone = getMilestoneById(id);

        if (milestone.getStatus() == MilestoneStatus.COMPLETED || milestone.getStatus() == MilestoneStatus.PAID) {
            throw new InvalidMilestoneStatusException("Completed or paid milestones cannot be reset");
        }

        milestone.setStatus(MilestoneStatus.IN_PROGRESS);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(null);
        milestone.setRejectionReason(null);
        milestone.setPaidAt(null);

        return milestoneRepository.save(milestone);
    }

    @Override
    @Transactional
    public Milestone validateMilestone(Long id, String approvalNotes) {
        Milestone milestone = getMilestoneById(id);
        return finalizeAcceptance(milestone, approvalNotes);
    }

    @Override
    public Milestone markMilestoneAsPaid(Long id) {
        Milestone milestone = getMilestoneById(id);

        if (milestone.getStatus() != MilestoneStatus.COMPLETED) {
            throw new InvalidMilestoneStatusException("Only completed milestones can be paid");
        }

        milestone.setStatus(MilestoneStatus.PAID);
        milestone.setIsCompleted(true);
        milestone.setPaidAt(LocalDateTime.now());

        return milestoneRepository.save(milestone);
    }

    private Milestone finalizeAcceptance(Milestone milestone, String approvalNotes) {
        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new InvalidMilestoneStatusException("Only submitted milestones can be accepted");
        }
        Mission mission = milestone.getMission();
        if (mission == null || mission.getClient() == null || mission.getAssignedFreelancer() == null) {
            throw new BusinessException("Client and freelancer must be set before validation");
        }

        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setIsCompleted(true);
        milestone.setCompletionDate(LocalDate.now());
        milestone.setCompletionNotes(approvalNotes);
        milestone.setRejectionReason(null);

        Milestone saved = milestoneRepository.save(milestone);

        try {
            paymentService.payMilestone(
                    saved.getId(),
                    mission.getClient().getId(),
                    mission.getAssignedFreelancer().getId(),
                    "WALLET",
                    approvalNotes != null ? approvalNotes : "Payment for milestone " + saved.getTitle()
            );
        } catch (BusinessException ex) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw ex;
        }

        saved.setStatus(MilestoneStatus.PAID);
        saved.setPaidAt(LocalDateTime.now());
        return milestoneRepository.save(saved);
    }

    private Freelancer requireAssignedFreelancer(Mission mission) {
        if (mission == null || mission.getAssignedFreelancer() == null) {
            throw new ForbiddenActionException("No freelancer assigned to this mission");
        }
        return mission.getAssignedFreelancer();
    }

    private void ensureFreelancerAccess(Milestone milestone, UserDetails currentUser) {
        Freelancer freelancer = requireAssignedFreelancer(milestone.getMission());
        if (currentUser == null || currentUser.getUsername() == null) {
            throw new ForbiddenActionException("Authenticated freelancer is required");
        }
        if (!freelancer.getEmail().equalsIgnoreCase(currentUser.getUsername())) {
            throw new ForbiddenActionException("Only the assigned freelancer can upload deliverables for this milestone");
        }
    }

    private void ensureClientAccess(Milestone milestone, UserDetails currentUser) {
        Client client = milestone.getMission() != null ? milestone.getMission().getClient() : null;
        if (client == null) {
            throw new ForbiddenActionException("Mission client not found");
        }
        if (currentUser == null || currentUser.getUsername() == null) {
            throw new ForbiddenActionException("Authenticated client is required");
        }
        if (!client.getEmail().equalsIgnoreCase(currentUser.getUsername())) {
            throw new ForbiddenActionException("Only the mission owner can perform this action");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }
        if (!FileUtils.isValidFileType(file.getOriginalFilename())) {
            throw new BusinessException("File type not allowed: " + file.getOriginalFilename());
        }
        if (!FileUtils.isValidFileSize(file.getSize())) {
            throw new BusinessException("File too large (max 10MB)");
        }
    }

    private void validateAmountAndDueDate(BigDecimal amount, LocalDate dueDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Milestone amount must be positive");
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Milestone due date cannot be in the past");
        }
    }
}
