package com.towork.milestone.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.mission.entity.Mission;
import com.towork.mission.repository.MissionRepository;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.service.MilestoneService;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final MissionRepository missionRepository;

    @PostMapping("/missions/{missionId}/milestones")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> createMilestone(@PathVariable Long missionId,
                                                           @RequestBody Milestone milestone,
                                                           Authentication authentication) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        verifyMissionOwnership(authentication, mission);
        milestone.setMission(mission);
        Milestone created = milestoneService.createMilestone(milestone);
        return ResponseEntity.ok(MessageResponse.success("Milestone created successfully", created));
    }

    @GetMapping("/milestones/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getMilestoneById(@PathVariable Long id,
                                                            Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyParticipant(authentication, milestone.getMission());
        return ResponseEntity.ok(MessageResponse.success("Milestone retrieved successfully", milestone));
    }

    @GetMapping("/milestones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> getAllMilestones(Pageable pageable) {
        Page<Milestone> milestones = milestoneService.getAllMilestones(pageable);
        return ResponseEntity.ok(MessageResponse.success("Milestones retrieved successfully", milestones));
    }

    @GetMapping("/missions/{missionId}/milestones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getMilestonesByMission(@PathVariable Long missionId,
                                                                  Authentication authentication) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        verifyParticipant(authentication, mission);
        List<Milestone> milestones = milestoneService.getMilestonesByMission(missionId);
        return ResponseEntity.ok(MessageResponse.success("Milestones retrieved successfully", milestones));
    }

    @GetMapping("/missions/{missionId}/milestones/ordered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getMilestonesByMissionOrdered(@PathVariable Long missionId,
                                                                         Authentication authentication) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        verifyParticipant(authentication, mission);
        List<Milestone> milestones = milestoneService.getMilestonesByMissionOrdered(missionId);
        return ResponseEntity.ok(MessageResponse.success("Milestones retrieved successfully", milestones));
    }

    @GetMapping("/missions/{missionId}/milestones/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getCompletedMilestones(@PathVariable Long missionId,
                                                                  Authentication authentication) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        verifyParticipant(authentication, mission);
        List<Milestone> milestones = milestoneService.getCompletedMilestones(missionId);
        return ResponseEntity.ok(MessageResponse.success("Completed milestones retrieved successfully", milestones));
    }

    @GetMapping("/missions/{missionId}/milestones/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> getPendingMilestones(@PathVariable Long missionId,
                                                                Authentication authentication) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found"));
        verifyParticipant(authentication, mission);
        List<Milestone> milestones = milestoneService.getPendingMilestones(missionId);
        return ResponseEntity.ok(MessageResponse.success("Pending milestones retrieved successfully", milestones));
    }

    @PostMapping("/milestones/{id}/deliver")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> deliverMilestone(@PathVariable Long id,
                                                            @RequestParam(value = "completionNotes", required = false) String completionNotes,
                                                            Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyFreelancer(authentication, milestone.getMission());
        Milestone updated = milestoneService.deliverMilestone(id, completionNotes);
        return ResponseEntity.ok(MessageResponse.success("Milestone marked as delivered", updated));
    }

    @PutMapping("/milestones/{id}/pending")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> markMilestoneAsPending(@PathVariable Long id,
                                                                  Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyMissionOwnership(authentication, milestone.getMission());
        Milestone updated = milestoneService.markMilestoneAsPending(id);
        return ResponseEntity.ok(MessageResponse.success("Milestone marked as pending", updated));
    }

    @PostMapping("/milestones/{id}/validate")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> validateMilestone(@PathVariable Long id,
                                                             @RequestParam(value = "approvalNotes", required = false) String approvalNotes,
                                                             Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyMissionOwnership(authentication, milestone.getMission());
        Milestone updated = milestoneService.validateMilestone(id, approvalNotes);
        return ResponseEntity.ok(MessageResponse.success("Milestone validated successfully", updated));
    }

    @PostMapping("/milestones/{id}/refuse")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> rejectMilestone(@PathVariable Long id,
                                                           @RequestParam(value = "reason", required = false) String reason,
                                                           Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyMissionOwnership(authentication, milestone.getMission());
        UserDetails userDetails = resolveUser(authentication);
        Milestone updated = milestoneService.rejectMilestone(id, reason, userDetails);
        return ResponseEntity.ok(MessageResponse.success("Milestone rejected", updated));
    }

    @DeleteMapping("/milestones/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<MessageResponse> deleteMilestone(@PathVariable Long id,
                                                           Authentication authentication) {
        Milestone milestone = milestoneService.getMilestoneById(id);
        verifyMissionOwnership(authentication, milestone.getMission());
        milestoneService.deleteMilestone(id);
        return ResponseEntity.ok(MessageResponse.success("Milestone deleted successfully"));
    }

    // ---------- Security helpers ----------

    private void verifyMissionOwnership(Authentication authentication, Mission mission) {
        if (authentication == null || mission == null) {
            throw new BusinessException("Access denied");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        Client owner = mission.getClient();
        if (isAdmin) {
            return;
        }
        String email = authentication.getName();
        if (owner == null || !email.equalsIgnoreCase(owner.getEmail())) {
            throw new BusinessException("You can only manage your own missions");
        }
    }

    private void verifyFreelancer(Authentication authentication, Mission mission) {
        if (authentication == null || mission == null) {
            throw new BusinessException("Access denied");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        if (isAdmin) {
            return;
        }
        String email = authentication.getName();
        Freelancer assigned = mission.getAssignedFreelancer();
        if (assigned == null || !email.equalsIgnoreCase(assigned.getEmail())) {
            throw new BusinessException("Only the assigned freelancer can deliver this milestone");
        }
    }

    private void verifyParticipant(Authentication authentication, Mission mission) {
        if (authentication == null || mission == null) {
            throw new BusinessException("Access denied");
        }
        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        boolean clientSide = mission.getClient() != null
                && email.equalsIgnoreCase(mission.getClient().getEmail());
        boolean freelancerSide = mission.getAssignedFreelancer() != null
                && email.equalsIgnoreCase(mission.getAssignedFreelancer().getEmail());
        if (!(isAdmin || clientSide || freelancerSide)) {
            throw new BusinessException("You are not allowed to access these milestones");
        }
    }

    private UserDetails resolveUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails;
        }
        if (authentication == null) {
            return null;
        }
        return User.withUsername(authentication.getName())
                .password("N/A")
                .authorities(authentication.getAuthorities())
                .build();
    }
}
