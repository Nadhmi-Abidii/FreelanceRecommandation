package com.towork.candidature.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.user.entity.Freelancer;
import com.towork.user.entity.Client;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import com.towork.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.candidature.dto.CandidatureMessageResponse;
import com.towork.candidature.dto.CreateCandidatureMessageRequest;
import com.towork.candidature.dto.CreateCandidatureRequest;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.entity.CandidatureMessage;
import com.towork.candidature.entity.CandidatureMessageAuthor;
import com.towork.candidature.entity.CandidatureStatus;
import com.towork.candidature.mapper.CandidatureMapper;
import com.towork.candidature.service.CandidatureService;

@RestController
@RequestMapping("/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;
    private final FreelancerRepository freelancerRepository;
    private final ClientRepository clientRepository;
    private final MissionRepository missionRepository;

    @PostMapping
    public ResponseEntity<MessageResponse> createCandidature(@RequestBody CreateCandidatureRequest request) {
        Freelancer freelancer = freelancerRepository.findById(request.getFreelancerId())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));

        Candidature candidature = new Candidature();
        candidature.setFreelancer(freelancer);
        candidature.setMission(mission);
        candidature.setProposedPrice(request.getProposedPrice());
        candidature.setProposedDuration(request.getProposedDuration());
        candidature.setCoverLetter(request.getCoverLetter());
        candidature.setResumeUrl(request.getResumeUrl());

        Candidature createdCandidature = candidatureService.createCandidature(candidature);
        return ResponseEntity.ok(MessageResponse.success("Candidature created successfully", CandidatureMapper.toDto(createdCandidature)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getCandidatureById(@PathVariable Long id) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        return ResponseEntity.ok(MessageResponse.success("Candidature retrieved successfully", CandidatureMapper.toDto(candidature)));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllCandidatures(Pageable pageable) {
        Page<Candidature> candidatures = candidatureService.getAllCandidatures(pageable);
        return ResponseEntity.ok(MessageResponse.success("Candidatures retrieved successfully", candidatures.stream().map(CandidatureMapper::toDto).toList()));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<MessageResponse> getCandidaturesByFreelancer(@PathVariable Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        List<Candidature> candidatures = candidatureService.getCandidaturesByFreelancer(freelancer);
        return ResponseEntity.ok(MessageResponse.success("Candidatures retrieved successfully", candidatures.stream().map(CandidatureMapper::toDto).toList()));
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<MessageResponse> getCandidaturesByMission(@PathVariable Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        List<Candidature> candidatures = candidatureService.getCandidaturesByMission(mission);
        return ResponseEntity.ok(MessageResponse.success("Candidatures retrieved successfully", candidatures.stream().map(CandidatureMapper::toDto).toList()));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<MessageResponse> addMessage(@PathVariable Long id,
                                                      @RequestBody CreateCandidatureMessageRequest request,
                                                      Authentication authentication) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        CandidatureMessageAuthor author = resolveAuthor(candidature, authentication);
        CandidatureMessage message = candidatureService.addMessage(id, author, request.getContent(), request.getResumeUrl());
        return ResponseEntity.ok(MessageResponse.success("Candidature message created successfully", CandidatureMapper.toMessageDto(message)));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<MessageResponse> getMessages(@PathVariable Long id, Authentication authentication) {
        Candidature candidature = candidatureService.getCandidatureById(id);
        ensureParticipant(candidature, authentication);
        List<CandidatureMessageResponse> messages = candidatureService.getMessages(id).stream()
                .map(CandidatureMapper::toMessageDto)
                .toList();
        return ResponseEntity.ok(MessageResponse.success("Candidature messages retrieved successfully", messages));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> updateCandidatureStatus(
            @PathVariable Long id,
            @RequestParam CandidatureStatus status,
            @RequestParam(required = false) String clientMessage,
            Authentication authentication) {
        Long clientId = resolveClientId(authentication);
        Candidature candidature = candidatureService.updateCandidatureStatus(id, status, clientMessage, clientId);
        return ResponseEntity.ok(MessageResponse.success("Candidature status updated successfully", CandidatureMapper.toDto(candidature)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> acceptCandidature(
            @PathVariable Long id,
            @RequestParam(required = false) String clientMessage,
            Authentication authentication) {
        Long clientId = resolveClientId(authentication);
        Candidature candidature = candidatureService.updateCandidatureStatus(id, CandidatureStatus.ACCEPTED, clientMessage, clientId);
        return ResponseEntity.ok(MessageResponse.success("Candidature accepted successfully", CandidatureMapper.toDto(candidature)));
    }

    @PostMapping("/{id}/refuse")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> refuseCandidature(
            @PathVariable Long id,
            @RequestParam(required = false) String clientMessage,
            Authentication authentication) {
        Long clientId = resolveClientId(authentication);
        Candidature candidature = candidatureService.updateCandidatureStatus(id, CandidatureStatus.REJECTED, clientMessage, clientId);
        return ResponseEntity.ok(MessageResponse.success("Candidature refused successfully", CandidatureMapper.toDto(candidature)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCandidature(@PathVariable Long id) {
        candidatureService.deleteCandidature(id);
        return ResponseEntity.ok(MessageResponse.success("Candidature deleted successfully"));
    }

    private void ensureParticipant(Candidature candidature, Authentication authentication) {
        resolveAuthor(candidature, authentication);
    }

    private CandidatureMessageAuthor resolveAuthor(Candidature candidature, Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException("Authentication is required to access this conversation");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority()));
        String email = authentication.getName();
        boolean isClient = candidature.getMission() != null
                && candidature.getMission().getClient() != null
                && email.equalsIgnoreCase(candidature.getMission().getClient().getEmail());
        boolean isFreelancer = candidature.getFreelancer() != null
                && email.equalsIgnoreCase(candidature.getFreelancer().getEmail());

        if (isClient) {
            return CandidatureMessageAuthor.CLIENT;
        }
        if (isFreelancer) {
            return CandidatureMessageAuthor.FREELANCER;
        }
        if (isAdmin) {
            return CandidatureMessageAuthor.CLIENT;
        }
        throw new BusinessException("You cannot access this candidature conversation");
    }

    private Long resolveClientId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return clientRepository.findByEmail(authentication.getName())
                .map(Client::getId)
                .orElse(null);
    }
}
