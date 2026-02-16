package com.towork.candidature.service.impl;

import com.towork.ai.dto.AiModerationResponse;
import com.towork.ai.service.AiModerationService;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.mission.entity.MissionStatus;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;
import com.towork.candidature.entity.CandidatureMessageAuthor;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.candidature.repository.CandidatureMessageRepository;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.candidature.service.CandidatureService;
import com.towork.conversation.entity.Message;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidatureServiceImpl implements CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final FreelancerRepository freelancerRepository;
    private final MissionRepository missionRepository;
    private final CandidatureMessageRepository candidatureMessageRepository;
    private final AiModerationService aiModerationService;

    @Override
    public Candidature createCandidature(Candidature candidature) {
        Freelancer freelancer = resolveFreelancer(candidature);
        Mission mission = resolveMission(candidature);

        if (hasFreelancerAppliedToMission(freelancer, mission)) {
            throw new BusinessException("You have already applied to this mission");
        }

        if (mission.getStatus() == MissionStatus.CANCELLED || mission.getStatus() == MissionStatus.COMPLETED) {
            throw new BusinessException("This mission is no longer accepting applications");
        }

        if (mission.getAssignedFreelancer() != null && !mission.getAssignedFreelancer().getId().equals(freelancer.getId())) {
            throw new BusinessException("Mission already assigned to another freelancer");
        }

        if (!Boolean.TRUE.equals(freelancer.getIsAvailable())) {
            throw new BusinessException("You are currently not available for new projects");
        }

        candidature.setFreelancer(freelancer);
        candidature.setMission(mission);
        candidature.setStatus(CandidatureStatus.PENDING);
        Candidature created = candidatureRepository.save(candidature);

        if (created.getCoverLetter() != null && !created.getCoverLetter().isBlank()) {
            CandidatureMessage intro = new CandidatureMessage();
            intro.setCandidature(created);
            intro.setAuthor(CandidatureMessageAuthor.FREELANCER);
            intro.setContent(created.getCoverLetter().trim());
            intro.setResumeUrl(created.getResumeUrl());
            AiModerationResponse moderation = aiModerationService.moderate(intro.getContent());
            applyModeration(intro, moderation);
            if (aiModerationService.shouldBlock(moderation)) {
                throw new BusinessException("Cover letter flagged as unsafe");
            }
            candidatureMessageRepository.save(intro);
        }

        return getCandidatureById(created.getId());
    }

    @Override
    public Candidature updateCandidature(Long id, Candidature candidature) {
        Candidature existingCandidature = getCandidatureById(id);
        
        // Business Logic: Only allow updates if candidature is still pending
        if (existingCandidature.getStatus() != CandidatureStatus.PENDING) {
            throw new BusinessException("Cannot update candidature that is not in pending status");
        }

        existingCandidature.setProposedPrice(candidature.getProposedPrice());
        existingCandidature.setProposedDuration(candidature.getProposedDuration());
        existingCandidature.setCoverLetter(candidature.getCoverLetter());
        existingCandidature.setResumeUrl(candidature.getResumeUrl());
        
        return candidatureRepository.save(existingCandidature);
    }

    @Override
    public void deleteCandidature(Long id) {
        Candidature candidature = getCandidatureById(id);
        
        // Business Logic: Only allow deletion if candidature is pending or withdrawn
        if (candidature.getStatus() == CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Cannot delete an accepted candidature");
        }

        candidature.setIsActive(false);
        candidatureRepository.save(candidature);
    }

    @Override
    public Candidature getCandidatureById(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature not found with id: " + id));
    }

    @Override
    public List<Candidature> getCandidaturesByFreelancer(Freelancer freelancer) {
        return candidatureRepository.findByFreelancer(freelancer);
    }

    @Override
    public List<Candidature> getCandidaturesByMission(Mission mission) {
        return candidatureRepository.findByMission(mission);
    }

    @Override
    public List<Candidature> getCandidaturesByStatus(CandidatureStatus status) {
        return candidatureRepository.findByStatus(status);
    }

    @Override
    public Page<Candidature> getAllCandidatures(Pageable pageable) {
        return candidatureRepository.findAll(pageable);
    }

    @Override
    public Candidature updateCandidatureStatus(Long id, CandidatureStatus status, String clientMessage, Long actingClientId) {
        Candidature candidature = getCandidatureById(id);
        Mission mission = candidature.getMission();

        if (mission == null) {
            throw new BusinessException("Candidature is not linked to a mission");
        }

        Client missionOwner = mission.getClient();
        if (actingClientId != null && missionOwner != null && !missionOwner.getId().equals(actingClientId)) {
            throw new BusinessException("Vous ne pouvez pas modifier une candidature pour une mission qui ne vous appartient pas");
        }

        // prevent invalid transitions from accepted to other statuses
        if (candidature.getStatus() == CandidatureStatus.ACCEPTED && status != CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Cannot change status of an accepted candidature");
        }

        // Accept flow: lock the mission and reject other candidatures
        if (status == CandidatureStatus.ACCEPTED) {
            if (mission.getStatus() == MissionStatus.CANCELLED || mission.getStatus() == MissionStatus.COMPLETED) {
                throw new BusinessException("Cannot accept candidature on a closed mission");
            }
            if (mission.getAssignedFreelancer() != null
                    && !mission.getAssignedFreelancer().getId().equals(candidature.getFreelancer().getId())) {
                throw new BusinessException("Mission already assigned to another freelancer");
            }

            candidature.setStatus(CandidatureStatus.ACCEPTED);
            candidature.setClientMessage(clientMessage);
            mission.setAssignedFreelancer(candidature.getFreelancer());
            mission.setStatus(MissionStatus.IN_PROGRESS);
            missionRepository.save(mission);

            // reject other candidatures for the same mission
            candidatureRepository.findByMission(mission).forEach(other -> {
                if (!other.getId().equals(candidature.getId())
                        && other.getStatus() != CandidatureStatus.REJECTED) {
                    other.setStatus(CandidatureStatus.REJECTED);
                    if (other.getClientMessage() == null || other.getClientMessage().isBlank()) {
                        other.setClientMessage("Un autre freelance a été retenu pour cette mission");
                    }
                    candidatureRepository.save(other);
                }
            });
            return candidatureRepository.save(candidature);
        }

        // Simple refusal or withdrawal
        if (status == CandidatureStatus.REJECTED || status == CandidatureStatus.WITHDRAWN) {
            candidature.setStatus(status);
            candidature.setClientMessage(clientMessage);
            return candidatureRepository.save(candidature);
        }

        candidature.setStatus(status);
        candidature.setClientMessage(clientMessage);
        return candidatureRepository.save(candidature);
    }

    @Override
    public boolean hasFreelancerAppliedToMission(Freelancer freelancer, Mission mission) {
        return candidatureRepository.findByFreelancerAndMission(freelancer, mission).isPresent();
    }

    @Override
    public CandidatureMessage addMessage(Long candidatureId, CandidatureMessageAuthor author, String content, String resumeUrl) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("Message content is required");
        }
        Candidature candidature = getCandidatureById(candidatureId);

        if (candidature.getStatus() != CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Messaging is available once the candidature is accepted");
        }

        CandidatureMessage message = new CandidatureMessage();
        message.setCandidature(candidature);
        message.setAuthor(author);
        message.setContent(content.trim());
        message.setResumeUrl((resumeUrl != null && !resumeUrl.isBlank()) ? resumeUrl : null);
        AiModerationResponse moderation = aiModerationService.moderate(message.getContent());
        applyModeration(message, moderation);
        if (aiModerationService.shouldBlock(moderation)) {
            throw new BusinessException("Message content flagged as unsafe");
        }

        return candidatureMessageRepository.save(message);
    }

    @Override
    public List<CandidatureMessage> getMessages(Long candidatureId) {
        Candidature candidature = getCandidatureById(candidatureId);
        return candidatureMessageRepository.findByCandidatureOrderByCreatedAtAsc(candidature);
    }

    private Freelancer resolveFreelancer(Candidature candidature) {
        Long freelancerId = candidature.getFreelancer() != null ? candidature.getFreelancer().getId() : null;
        if (freelancerId == null) {
            throw new BusinessException("Freelancer information is required");
        }

        return freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
    }

    private Mission resolveMission(Candidature candidature) {
        Long missionId = candidature.getMission() != null ? candidature.getMission().getId() : null;
        if (missionId == null) {
            throw new BusinessException("Mission information is required");
        }

        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + missionId));
    }

    private void applyModeration(CandidatureMessage message, AiModerationResponse moderation) {
        if (moderation == null) {
            return;
        }
        message.setIsFlagged(Boolean.TRUE.equals(moderation.getFlagged()));
        message.setFlagScore(moderation.getScore());
        message.setFlagLabel(moderation.getLabel());
        message.setFlagReason(moderation.getReason());
    }
}

