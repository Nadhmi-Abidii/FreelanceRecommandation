package com.towork.mission.controller;

import com.towork.ai.dto.AiFreelancerMatchDto;
import com.towork.ai.dto.AiSummaryResponse;
import com.towork.ai.service.AiFeatureService;
import com.towork.ai.service.AiMatchingService;
import com.towork.config.MessageResponse;
import com.towork.exception.ResourceNotFoundException;
import com.towork.candidature.entity.Candidature;
import com.towork.candidature.mapper.CandidatureMapper;
import com.towork.candidature.service.CandidatureService;
import com.towork.candidature.dto.CreateCandidatureRequest;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.mission.dto.MissionResponse;
import com.towork.mission.entity.Mission;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;
import com.towork.mission.mapper.MissionMapper;
import com.towork.mission.service.MissionService;

@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    private final ClientRepository clientRepository;
    private final CandidatureService candidatureService;
    private final FreelancerRepository freelancerRepository;
    private final AiMatchingService aiMatchingService;
    private final AiFeatureService aiFeatureService;

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> completeMission(@PathVariable Long id, Authentication authentication) {
        UserDetails userDetails = resolve(authentication);
        Mission mission = missionService.completeMissionForFreelancer(id, userDetails);
        return ResponseEntity.ok(MessageResponse.success("Mission submitted for client closure", MissionMapper.toDto(mission)));
    }

    @PostMapping("/{id}/submit-final")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> submitFinalDelivery(@PathVariable Long id, Authentication authentication) {
        Mission mission = missionService.submitFinalDelivery(id, resolve(authentication));
        return ResponseEntity.ok(MessageResponse.success("Final delivery submitted, waiting for client closure", MissionMapper.toDto(mission)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> closeMission(@PathVariable Long id, Authentication authentication) {
        Mission mission = missionService.closeMission(id, resolve(authentication));
        return ResponseEntity.ok(MessageResponse.success("Mission closed successfully", MissionMapper.toDto(mission)));
    }

    @PostMapping("/{missionId}/candidatures")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> applyToMission(@PathVariable Long missionId,
                                                          @RequestBody CreateCandidatureRequest request,
                                                          Authentication authentication) {
        Freelancer freelancer = freelancerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        Mission mission = missionService.getMissionById(missionId);

        Candidature candidature = new Candidature();
        candidature.setMission(mission);
        candidature.setFreelancer(freelancer);
        candidature.setProposedPrice(request.getProposedPrice());
        candidature.setProposedDuration(request.getProposedDuration());
        candidature.setCoverLetter(request.getCoverLetter());
        candidature.setResumeUrl(request.getResumeUrl());

        Candidature created = candidatureService.createCandidature(candidature);
        return ResponseEntity.ok(MessageResponse.success("Candidature created successfully", CandidatureMapper.toDto(created)));
    }

    // CREATE â€” derive client from authenticated user, ignore any client id coming
    // in body
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> createMission(@RequestBody Mission mission, Authentication auth) {
        // who is the logged-in user (email)
        final String email = auth.getName();

        // attach the client entity to the mission (required by your model)
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Only clients can create missions (no client found for user email)"));
        mission.setClient(client);

        Mission created = missionService.createMission(mission);
        return ResponseEntity.ok(MessageResponse.success("Mission created successfully", MissionMapper.toDto(created)));
    }

    // LIST mine â€” no id required
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> getMyMissions(Authentication auth) {
        final String email = auth.getName();
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No client account found for this user"));
        List<Mission> mine = missionService.getMissionsByClient(client.getId());
        List<MissionResponse> dto = mine.stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Missions retrieved successfully", dto));
    }

    // ===== Existing endpoints (keep them) =====

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMissionById(@PathVariable Long id) {
        Mission mission = missionService.getMissionById(id);
        return ResponseEntity
                .ok(MessageResponse.success("Mission retrieved successfully", MissionMapper.toDto(mission)));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<MessageResponse> getMissionRecommendations(@PathVariable Long id,
                                                                     @RequestParam(required = false) Integer limit) {
        List<AiFreelancerMatchDto> matches = aiMatchingService.recommendFreelancers(id, limit);
        return ResponseEntity.ok(MessageResponse.success("Recommendations generated", matches));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<MessageResponse> getMissionSummary(@PathVariable Long id,
                                                             @RequestParam(required = false) String language) {
        Mission mission = missionService.getMissionById(id);
        AiSummaryResponse summary = aiFeatureService.summarizeMission(
                mission.getTitle(),
                mission.getDescription(),
                mission.getRequirements(),
                mission.getStatus() != null ? mission.getStatus().name() : null,
                language
        );
        return ResponseEntity.ok(MessageResponse.success("Mission summary generated", summary));
    }

    @GetMapping
    public ResponseEntity<MessageResponse> getAllMissions(Pageable pageable) {
        Page<Mission> missions = missionService.getAllMissions(pageable);
        Page<MissionResponse> dtoPage = missions.map(MissionMapper::toDto);
        return ResponseEntity.ok(MessageResponse.success("Missions retrieved successfully", dtoPage));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN')") // optional: restrict direct by-id access to admin only
    public ResponseEntity<MessageResponse> getMissionsByClient(@PathVariable Long clientId) {
        List<MissionResponse> missions = missionService.getMissionsByClient(clientId)
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Missions retrieved successfully", missions));
    }

    @GetMapping("/domaine/{domaineId}")
    public ResponseEntity<MessageResponse> getMissionsByDomaine(@PathVariable Long domaineId) {
        List<MissionResponse> missions = missionService.getMissionsByDomaine(domaineId)
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Missions retrieved successfully", missions));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<MessageResponse> getMissionsByStatus(@PathVariable MissionStatus status) {
        List<MissionResponse> missions = missionService.getMissionsByStatus(status)
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Missions retrieved successfully", missions));
    }

    @GetMapping("/published")
    public ResponseEntity<MessageResponse> getPublishedMissions() {
        List<MissionResponse> missions = missionService.getPublishedMissions()
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Published missions retrieved successfully", missions));
    }

    @GetMapping("/urgent")
    public ResponseEntity<MessageResponse> getUrgentMissions() {
        List<MissionResponse> missions = missionService.getUrgentMissions()
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Urgent missions retrieved successfully", missions));
    }

    @GetMapping("/search")
    public ResponseEntity<MessageResponse> searchMissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long domaineId,
            @RequestParam(required = false) TypeTravail typeTravail,
            @RequestParam(required = false) NiveauExperience niveauExperience) {
        List<MissionResponse> missions = missionService
                .searchMissions(keyword, domaineId, typeTravail, niveauExperience)
                .stream().map(MissionMapper::toDto).toList();
        return ResponseEntity.ok(MessageResponse.success("Search results retrieved successfully", missions));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MessageResponse> updateMissionStatus(@PathVariable Long id,
            @RequestParam MissionStatus status) {
        Mission mission = missionService.updateMissionStatus(id, status);
        return ResponseEntity
                .ok(MessageResponse.success("Mission status updated successfully", MissionMapper.toDto(mission)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateMission(@PathVariable Long id, @RequestBody Mission mission) {
        Mission updated = missionService.updateMission(id, mission);
        return ResponseEntity.ok(MessageResponse.success("Mission updated successfully", MissionMapper.toDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteMission(@PathVariable Long id) {
        missionService.deleteMission(id);
        return ResponseEntity.ok(MessageResponse.success("Mission deleted successfully"));
    }

    private UserDetails resolve(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
