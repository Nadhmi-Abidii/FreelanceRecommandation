package com.towork.milestone.service;

import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.file.FileStorageService;
import com.towork.file.FileUtils;
import com.towork.milestone.dto.MilestoneDeliverableDto;
import com.towork.milestone.dto.MilestoneDto;
import com.towork.milestone.mapper.MilestoneMapper;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.wallet.service.PaymentService;
import com.towork.user.entity.Freelancer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;
import com.towork.milestone.entity.MilestoneStatus;
import com.towork.milestone.repository.MilestoneDeliverableRepository;
import com.towork.milestone.repository.MilestoneRepository;

@Service
@RequiredArgsConstructor
public class MilestoneWorkflowService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneDeliverableRepository deliverableRepository;
    private final MissionRepository missionRepository;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public List<MilestoneDto> getMilestonesForFreelancer(Long missionId, String freelancerEmail) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        if (mission.getAssignedFreelancer() == null || !mission.getAssignedFreelancer().getEmail().equalsIgnoreCase(freelancerEmail)) {
            throw new BusinessException("You are not assigned to this mission");
        }
        List<Milestone> milestones = milestoneRepository.findByMissionAndIsActiveTrueOrderByOrderIndexAsc(mission);
        return mapWithDeliverables(milestones);
    }

    @Transactional(readOnly = true)
    public List<MilestoneDto> getMilestonesForClient(Long missionId, String clientEmail) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        if (mission.getClient() == null || !mission.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new BusinessException("You do not own this mission");
        }
        List<Milestone> milestones = milestoneRepository.findByMissionAndIsActiveTrueOrderByOrderIndexAsc(mission);
        return mapWithDeliverables(milestones);
    }

    @Transactional
    public List<MilestoneDeliverableDto> uploadDeliverables(Long milestoneId, List<MultipartFile> files, String comment, String freelancerEmail) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("At least one file is required");
        }
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        Mission mission = milestone.getMission();
        if (mission == null || mission.getAssignedFreelancer() == null
                || !mission.getAssignedFreelancer().getEmail().equalsIgnoreCase(freelancerEmail)) {
            throw new BusinessException("You cannot upload files for this milestone");
        }
        if (milestone.getStatus() == MilestoneStatus.PAID || milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new BusinessException("Cannot upload deliverables for a completed or paid milestone");
        }

        Freelancer uploader = mission.getAssignedFreelancer();
        List<MilestoneDeliverable> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            String storageKey;
            try {
                storageKey = fileStorageService.storeFile(file);
            } catch (IOException e) {
                throw new BusinessException("Cannot store file " + file.getOriginalFilename(), e);
            }
            MilestoneDeliverable deliverable = new MilestoneDeliverable();
            deliverable.setMilestone(milestone);
            deliverable.setUploader(uploader);
            deliverable.setFileName(file.getOriginalFilename());
            deliverable.setStorageKey(storageKey);
            deliverable.setContentType(file.getContentType());
            deliverable.setComment(comment);
            saved.add(deliverableRepository.save(deliverable));
        }

        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone.setIsCompleted(false);
        milestone.setCompletionDate(null);
        milestone.setCompletionNotes(comment);
        milestone.setRejectionReason(null);
        milestoneRepository.save(milestone);

        return saved.stream()
                .map(d -> MilestoneMapper.toDeliverableDto(d, "/api/milestones/deliverables"))
                .toList();
    }

    @Transactional
    public MilestoneDto validateMilestone(Long milestoneId, String clientEmail, String approvalNotes) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        Mission mission = milestone.getMission();
        if (mission == null || mission.getClient() == null || !mission.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new BusinessException("You cannot validate this milestone");
        }
        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new BusinessException("Only submitted milestones can be validated");
        }
        if (mission.getAssignedFreelancer() == null) {
            throw new BusinessException("No freelancer assigned to this mission");
        }

        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setIsCompleted(true);
        milestone.setCompletionDate(LocalDate.now());
        milestone.setCompletionNotes(approvalNotes);
        milestone.setRejectionReason(null);
        milestoneRepository.save(milestone);

        try {
            paymentService.payMilestone(
                    milestone.getId(),
                    mission.getClient().getId(),
                    mission.getAssignedFreelancer().getId(),
                    "WALLET",
                    approvalNotes != null ? approvalNotes : "Paiement du jalon " + milestone.getTitle()
            );
        } catch (BusinessException ex) {
            milestone.setStatus(MilestoneStatus.SUBMITTED);
            milestoneRepository.save(milestone);
            throw ex;
        }

        milestone.setStatus(MilestoneStatus.PAID);
        milestone.setPaidAt(java.time.LocalDateTime.now());
        Milestone refreshed = milestoneRepository.save(milestone);
        List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(refreshed);
        return MilestoneMapper.toDto(refreshed, deliverables, "/api/milestones/deliverables");
    }

    @Transactional
    public MilestoneDto rejectMilestone(Long milestoneId, String clientEmail, String reason) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        Mission mission = milestone.getMission();
        if (mission == null || mission.getClient() == null || !mission.getClient().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new BusinessException("You cannot reject this milestone");
        }
        if (milestone.getStatus() != MilestoneStatus.SUBMITTED) {
            throw new BusinessException("Only submitted milestones can be rejected");
        }

        milestone.setStatus(MilestoneStatus.REJECTED);
        milestone.setIsCompleted(false);
        milestone.setCompletionNotes(null);
        milestone.setRejectionReason(reason);
        milestoneRepository.save(milestone);

        List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(milestone);
        return MilestoneMapper.toDto(milestone, deliverables, "/api/milestones/deliverables");
    }

    private List<MilestoneDto> mapWithDeliverables(List<Milestone> milestones) {
        return milestones.stream()
                .map(m -> {
                    List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(m);
                    return MilestoneMapper.toDto(m, deliverables, "/api/milestones/deliverables");
                })
                .toList();
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
}
