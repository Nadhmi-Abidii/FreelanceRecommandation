package com.towork.mission.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ForbiddenActionException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneStatus;
import com.towork.milestone.repository.MilestoneRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.DomaineRepository;
import com.towork.wallet.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;
import com.towork.mission.repository.MissionRepository;
import com.towork.mission.service.MissionService;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final ClientRepository clientRepository;
    private final DomaineRepository domaineRepository;
    private final CandidatureRepository candidatureRepository; // 👈 AJOUT ICI
    private final MilestoneRepository milestoneRepository; // 👈 AJOUT ICI
    private final PaymentService paymentService;


    @Override
    public Mission createMission(Mission mission) {
        // Business Logic: Validate client exists and is verified
        Client client = clientRepository.findById(mission.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        
        

        // Business Logic: Validate domaine exists and is active
        Domaine domaine = domaineRepository.findById(mission.getDomaine().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Domaine not found"));
        
        if (!domaine.getIsActive()) {
            throw new BusinessException("Cannot create mission in inactive domaine");
        }

        // Business Logic: Validate budget constraints
        if (mission.getBudgetMin() != null && mission.getBudgetMax() != null) {
            if (mission.getBudgetMin().compareTo(mission.getBudgetMax()) > 0) {
                throw new BusinessException("Minimum budget cannot be greater than maximum budget");
            }
        }

        // Publish directly if required fields are present, otherwise refuse creation
        if (mission.getTitle() == null || mission.getTitle().trim().isEmpty()) {
            throw new BusinessException("Mission title is required to publish");
        }
        if (mission.getDescription() == null || mission.getDescription().trim().isEmpty()) {
            throw new BusinessException("Mission description is required to publish");
        }
        if (mission.getBudgetMin() == null && mission.getBudgetMax() == null) {
            throw new BusinessException("Budget information is required to publish");
        }

        mission.setStatus(MissionStatus.PUBLISHED);
        
        return missionRepository.save(mission);
    }

    @Override
    public Mission updateMission(Long id, Mission mission) {
        Mission existingMission = getMissionById(id);
        
        // Business Logic: Only allow updates if mission is in draft or published state
        if (existingMission.getStatus() == MissionStatus.IN_PROGRESS ||
            existingMission.getStatus() == MissionStatus.PENDING_CLOSURE ||
            existingMission.getStatus() == MissionStatus.COMPLETED) {
            throw new BusinessException("Cannot update a mission that is in progress or completed");
        }

        existingMission.setTitle(mission.getTitle());
        existingMission.setDescription(mission.getDescription());
        existingMission.setRequirements(mission.getRequirements());
        existingMission.setBudgetMin(mission.getBudgetMin());
        existingMission.setBudgetMax(mission.getBudgetMax());
        existingMission.setBudgetType(mission.getBudgetType());
        existingMission.setTypeTravail(mission.getTypeTravail());
        existingMission.setNiveauExperience(mission.getNiveauExperience());
        existingMission.setDeadline(mission.getDeadline());
        existingMission.setEstimatedDuration(mission.getEstimatedDuration());
        existingMission.setSkillsRequired(mission.getSkillsRequired());
        existingMission.setIsUrgent(mission.getIsUrgent());
        existingMission.setAttachments(mission.getAttachments());
        
        return missionRepository.save(existingMission);
    }

    @Override
    public void deleteMission(Long id) {
        Mission mission = getMissionById(id);

        if (mission.getStatus() != MissionStatus.DRAFT) {
            throw new BusinessException("Cannot delete mission that is not in draft status");
        }

        candidatureRepository.deleteAllByMission(mission);
        milestoneRepository.deleteAllByMission(mission);
        mission.setIsActive(false);
        missionRepository.save(mission);
    }

    @Override
    public Mission getMissionById(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));
    }

    @Override
    public List<Mission> getMissionsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        return missionRepository.findByClient(client);
    }

    @Override
    public List<Mission> getMissionsByDomaine(Long domaineId) {
        Domaine domaine = domaineRepository.findById(domaineId)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine not found with id: " + domaineId));
        return missionRepository.findByDomaine(domaine);
    }

    @Override
    public List<Mission> getMissionsByStatus(MissionStatus status) {
        return missionRepository.findByStatus(status);
    }

    @Override
    public List<Mission> getPublishedMissions() {
        return missionRepository.findPublishedMissionsOrderByCreatedAt();
    }

    @Override
    public List<Mission> getUrgentMissions() {
        return missionRepository.findUrgentMissionsByStatus(MissionStatus.PUBLISHED);
    }

    @Override
    public Page<Mission> getAllMissions(Pageable pageable) {
        return missionRepository.findAll(pageable);
    }

    @Override
    public Mission updateMissionStatus(Long id, MissionStatus status) {
        Mission mission = getMissionById(id);
        
        // Business Logic: Validate status transitions
        if (mission.getStatus() == MissionStatus.COMPLETED && status != MissionStatus.COMPLETED) {
            throw new BusinessException("Cannot change status of a completed mission");
        }
        
        if (mission.getStatus() == MissionStatus.CANCELLED && status != MissionStatus.CANCELLED) {
            throw new BusinessException("Cannot reactivate a cancelled mission");
        }

        // Business Logic: If publishing mission, validate required fields
        if (status == MissionStatus.PUBLISHED) {
            if (mission.getTitle() == null || mission.getTitle().trim().isEmpty()) {
                throw new BusinessException("Mission title is required to publish");
            }
            if (mission.getDescription() == null || mission.getDescription().trim().isEmpty()) {
                throw new BusinessException("Mission description is required to publish");
            }
            if (mission.getBudgetMin() == null && mission.getBudgetMax() == null) {
                throw new BusinessException("Budget information is required to publish");
            }
        }

        mission.setStatus(status);
        return missionRepository.save(mission);
    }

    @Override
    public Mission completeMissionForFreelancer(Long missionId, UserDetails currentUser) {
        return submitFinalDelivery(missionId, currentUser);
    }

    @Override
    public Mission submitFinalDelivery(Long missionId, UserDetails currentUser) {
        Mission mission = getMissionById(missionId);
        ensureFreelancerAccess(mission, currentUser);

        if (!Boolean.TRUE.equals(mission.getIsActive())) {
            throw new BusinessException("Mission is inactive");
        }
        if (mission.getStatus() == MissionStatus.COMPLETED) {
            throw new BusinessException("Mission already completed");
        }
        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new BusinessException("Cancelled missions cannot be completed");
        }
        if (mission.getStatus() == MissionStatus.PENDING_CLOSURE) {
            return mission;
        }
        if (mission.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new BusinessException("Mission must be in progress to submit the final delivery");
        }

        List<Milestone> milestones = milestoneRepository.findByMissionAndIsActiveTrue(mission);
        boolean hasBlockingMilestones = milestones != null && milestones.stream()
                .anyMatch(ms -> ms.getStatus() != MilestoneStatus.COMPLETED && ms.getStatus() != MilestoneStatus.PAID);
        if (hasBlockingMilestones) {
            throw new BusinessException("All milestones must be validated before requesting closure");
        }

        mission.setStatus(MissionStatus.PENDING_CLOSURE);
        return missionRepository.save(mission);
    }

    @Override
    public Mission closeMission(Long missionId, UserDetails currentUser) {
        Mission mission = getMissionById(missionId);
        ensureClientAccess(mission, currentUser);

        if (!Boolean.TRUE.equals(mission.getIsActive())) {
            throw new BusinessException("Mission is inactive");
        }
        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new BusinessException("Cancelled missions cannot be closed");
        }
        if (mission.getStatus() == MissionStatus.COMPLETED) {
            throw new BusinessException("Mission already closed");
        }
        if (mission.getAssignedFreelancer() == null) {
            throw new BusinessException("Mission has no assigned freelancer");
        }
        if (mission.getStatus() != MissionStatus.PENDING_CLOSURE && mission.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new BusinessException("Mission is not ready for closure");
        }

        List<Milestone> milestones = milestoneRepository.findByMissionAndIsActiveTrue(mission);
        for (Milestone milestone : milestones) {
            MilestoneStatus status = milestone.getStatus();
            if (status == MilestoneStatus.DRAFT || status == MilestoneStatus.IN_PROGRESS || status == MilestoneStatus.SUBMITTED) {
                throw new BusinessException("All milestones must be validated before closing the mission");
            }
            if (status == MilestoneStatus.REJECTED) {
                throw new BusinessException("A rejected milestone is pending rework before closure");
            }
            if (status == MilestoneStatus.COMPLETED) {
                paymentService.payMilestone(
                        milestone.getId(),
                        mission.getClient().getId(),
                        mission.getAssignedFreelancer().getId(),
                        "WALLET",
                        "Paiement final mission " + mission.getTitle()
                );
            }
        }

        mission.setStatus(MissionStatus.COMPLETED);
        return missionRepository.save(mission);
    }

    @Override
    public List<Mission> searchMissions(String keyword, Long domaineId, TypeTravail typeTravail, NiveauExperience niveauExperience) {
        // This would need a custom query implementation in the repository
        // For now, returning all missions as a placeholder
        return missionRepository.findAll();
    }

    private void ensureFreelancerAccess(Mission mission, UserDetails currentUser) {
        if (currentUser == null || currentUser.getUsername() == null) {
            throw new ForbiddenActionException("Authenticated freelancer is required");
        }
        if (mission.getAssignedFreelancer() == null) {
            throw new ForbiddenActionException("No freelancer assigned to this mission");
        }
        if (!mission.getAssignedFreelancer().getEmail().equalsIgnoreCase(currentUser.getUsername())) {
            throw new ForbiddenActionException("You are not the freelancer on this mission");
        }
    }

    private void ensureClientAccess(Mission mission, UserDetails currentUser) {
        if (currentUser == null || currentUser.getUsername() == null) {
            throw new ForbiddenActionException("Authenticated client is required");
        }
        Client client = mission.getClient();
        if (client == null || client.getEmail() == null) {
            throw new ForbiddenActionException("Mission client not found");
        }
        if (!client.getEmail().equalsIgnoreCase(currentUser.getUsername())) {
            throw new ForbiddenActionException("Only the mission owner can perform this action");
        }
    }
}
