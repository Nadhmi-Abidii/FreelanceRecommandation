package com.towork.candidature.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.candidature.dto.CandidatureResponse;
import com.towork.candidature.mapper.CandidatureMapper;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.towork.candidature.service.CandidatureService;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MyCandidatureController {

    private final CandidatureService candidatureService;
    private final FreelancerRepository freelancerRepository;
    private final ClientRepository clientRepository;
    private final MissionRepository missionRepository;

    @GetMapping("/candidatures")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> getMyCandidatures(Authentication authentication) {
        Freelancer freelancer = freelancerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));

        List<CandidatureResponse> candidatures = candidatureService.getCandidaturesByFreelancer(freelancer)
                .stream()
                .map(CandidatureMapper::toDto)
                .toList();

        return ResponseEntity.ok(MessageResponse.success("Candidatures retrieved successfully", candidatures));
    }

    @GetMapping("/missions/{missionId}/candidatures")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> getCandidaturesForMyMission(@PathVariable Long missionId,
                                                                       Authentication authentication) {
        Client client = clientRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        if (mission.getClient() == null || !mission.getClient().getId().equals(client.getId())) {
            throw new BusinessException("You cannot view candidatures for a mission you do not own");
        }

        List<CandidatureResponse> candidatures = candidatureService.getCandidaturesByMission(mission)
                .stream()
                .map(CandidatureMapper::toDto)
                .toList();
        return ResponseEntity.ok(MessageResponse.success("Candidatures retrieved successfully", candidatures));
    }
}
