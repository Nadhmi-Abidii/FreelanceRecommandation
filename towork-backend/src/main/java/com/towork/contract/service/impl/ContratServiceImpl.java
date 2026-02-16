package com.towork.contract.service.impl;

import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.candidature.repository.CandidatureRepository;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.towork.contract.entity.Contrat;
import com.towork.contract.entity.EtatContrat;
import com.towork.contract.repository.ContratRepository;
import com.towork.contract.service.ContratService;
import com.towork.mission.entity.MissionStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratServiceImpl implements ContratService {

    private final ContratRepository contratRepository;
    private final CandidatureRepository candidatureRepository;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final MissionRepository missionRepository;

    @Override
    public Contrat createContrat(Contrat contrat) {
        // Business Logic: Validate that the candidature exists and is accepted
        Candidature candidature = candidatureRepository.findByFreelancerAndMission(
                contrat.getFreelancer(), contrat.getMission())
                .orElseThrow(() -> new ResourceNotFoundException("No candidature found for this freelancer and mission"));

        if (candidature.getStatus() != CandidatureStatus.ACCEPTED) {
            throw new BusinessException("Cannot create contract from a non-accepted candidature");
        }

        // Business Logic: Check if contract already exists for this mission
        if (contratRepository.findByClientAndFreelancer(contrat.getClient(), contrat.getFreelancer())
                .stream().anyMatch(c -> c.getMission().getId().equals(contrat.getMission().getId()))) {
            throw new BusinessException("A contract already exists for this mission");
        }

        // Business Logic: Set initial contract state
        contrat.setEtat(EtatContrat.DRAFT);
        
        return contratRepository.save(contrat);
    }

    @Override
    public Contrat updateContrat(Long id, Contrat contrat) {
        Contrat existingContrat = getContratById(id);
        
        // Business Logic: Only allow updates if contract is in draft or active state
        if (existingContrat.getEtat() == EtatContrat.COMPLETED || existingContrat.getEtat() == EtatContrat.CANCELLED) {
            throw new BusinessException("Cannot update a completed or cancelled contract");
        }

        existingContrat.setTitle(contrat.getTitle());
        existingContrat.setDescription(contrat.getDescription());
        existingContrat.setTotalAmount(contrat.getTotalAmount());
        existingContrat.setStartDate(contrat.getStartDate());
        existingContrat.setEndDate(contrat.getEndDate());
        existingContrat.setTerms(contrat.getTerms());
        existingContrat.setPaymentTerms(contrat.getPaymentTerms());
        existingContrat.setMilestoneBased(contrat.getMilestoneBased());
        
        return contratRepository.save(existingContrat);
    }

    @Override
    public void deleteContrat(Long id) {
        Contrat contrat = getContratById(id);
        
        // Business Logic: Only allow deletion if contract is in draft state
        if (contrat.getEtat() != EtatContrat.DRAFT) {
            throw new BusinessException("Cannot delete a contract that is not in draft state");
        }

        contrat.setIsActive(false);
        contratRepository.save(contrat);
    }

    @Override
    public Contrat getContratById(Long id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat not found with id: " + id));
    }

    @Override
    public List<Contrat> getContratsByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        return contratRepository.findByClient(client);
    }

    @Override
    public List<Contrat> getContratsByFreelancer(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found with id: " + freelancerId));
        return contratRepository.findByFreelancer(freelancer);
    }

    @Override
    public List<Contrat> getContratsByEtat(EtatContrat etat) {
        return contratRepository.findByEtat(etat);
    }

    @Override
    public Page<Contrat> getAllContrats(Pageable pageable) {
        return contratRepository.findAll(pageable);
    }

    @Override
    public Contrat updateContratEtat(Long id, EtatContrat etat) {
        Contrat contrat = getContratById(id);
        
        // Business Logic: Validate state transitions
        if (contrat.getEtat() == EtatContrat.COMPLETED && etat != EtatContrat.COMPLETED) {
            throw new BusinessException("Cannot change status of a completed contract");
        }
        
        if (contrat.getEtat() == EtatContrat.CANCELLED && etat != EtatContrat.CANCELLED) {
            throw new BusinessException("Cannot reactivate a cancelled contract");
        }

        contrat.setEtat(etat);
        
        // Business Logic: If contract becomes active, update mission status
        if (etat == EtatContrat.ACTIVE) {
            Mission mission = contrat.getMission();
            mission.setStatus(com.towork.mission.entity.MissionStatus.IN_PROGRESS);
            missionRepository.save(mission);
        }
        
        // Business Logic: If contract is completed, update mission status
        if (etat == EtatContrat.COMPLETED) {
            Mission mission = contrat.getMission();
            mission.setStatus(com.towork.mission.entity.MissionStatus.COMPLETED);
            missionRepository.save(mission);
        }
        
        return contratRepository.save(contrat);
    }

    @Override
    public List<Contrat> getActiveContrats() {
        return contratRepository.findByEtat(EtatContrat.ACTIVE);
    }
}

