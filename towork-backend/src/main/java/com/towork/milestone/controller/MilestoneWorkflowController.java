package com.towork.milestone.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.BusinessException;
import com.towork.exception.ResourceNotFoundException;
import com.towork.file.FileStorageService;
import com.towork.milestone.dto.MilestoneDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;
import com.towork.milestone.repository.MilestoneDeliverableRepository;
import com.towork.milestone.service.MilestoneWorkflowService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MilestoneWorkflowController {

    private final MilestoneWorkflowService workflowService;
    private final MilestoneDeliverableRepository deliverableRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/freelancer/missions/{missionId}/milestones")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> getMilestonesForFreelancer(@PathVariable Long missionId,
                                                                      Authentication authentication) {
        List<MilestoneDto> milestones = workflowService.getMilestonesForFreelancer(missionId, authentication.getName());
        return ResponseEntity.ok(MessageResponse.success("Milestones retrieved", milestones));
    }

    @GetMapping("/client/missions/{missionId}/milestones")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> getMilestonesForClient(@PathVariable Long missionId,
                                                                  Authentication authentication) {
        List<MilestoneDto> milestones = workflowService.getMilestonesForClient(missionId, authentication.getName());
        return ResponseEntity.ok(MessageResponse.success("Milestones retrieved", milestones));
    }

    @PostMapping(value = "/freelancer/milestones/{milestoneId}/deliverables", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> uploadDeliverables(@PathVariable Long milestoneId,
                                                              @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                                              @RequestPart(value = "file", required = false) MultipartFile singleFile,
                                                              @RequestParam(value = "comment", required = false) String comment,
                                                              Authentication authentication) {
        List<MultipartFile> payload = new ArrayList<>();
        if (files != null) {
            payload.addAll(files);
        }
        if (singleFile != null) {
            payload.add(singleFile);
        }
        var deliverables = workflowService.uploadDeliverables(milestoneId, payload, comment, authentication.getName());
        return ResponseEntity.ok(MessageResponse.success("Deliverables uploaded", deliverables));
    }

    @PostMapping("/client/milestones/{milestoneId}/validate")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> validateMilestone(@PathVariable Long milestoneId,
                                                             @RequestParam(value = "approvalNotes", required = false) String approvalNotes,
                                                             Authentication authentication) {
        MilestoneDto dto = workflowService.validateMilestone(milestoneId, authentication.getName(), approvalNotes);
        return ResponseEntity.ok(MessageResponse.success("Milestone validated and paid", dto));
    }

    @PostMapping("/client/milestones/{milestoneId}/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> rejectMilestone(@PathVariable Long milestoneId,
                                                           @RequestParam(value = "reason", required = false) String reason,
                                                           Authentication authentication) {
        MilestoneDto dto = workflowService.rejectMilestone(milestoneId, authentication.getName(), reason);
        return ResponseEntity.ok(MessageResponse.success("Milestone rejected", dto));
    }

    @GetMapping("/milestones/deliverables/{deliverableId}/download")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<ByteArrayResource> downloadDeliverable(@PathVariable Long deliverableId,
                                                                  Authentication authentication) throws IOException {
        MilestoneDeliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable not found"));
        Milestone milestone = deliverable.getMilestone();
        if (milestone == null || milestone.getMission() == null) {
            throw new BusinessException("Invalid deliverable");
        }
        var mission = milestone.getMission();
        boolean isClient = mission.getClient() != null && mission.getClient().getEmail().equalsIgnoreCase(authentication.getName());
        boolean isFreelancer = mission.getAssignedFreelancer() != null && mission.getAssignedFreelancer().getEmail().equalsIgnoreCase(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()));
        if (!(isClient || isFreelancer || isAdmin)) {
            throw new BusinessException("You cannot access this file");
        }

        byte[] data = fileStorageService.loadFileAsBytes(deliverable.getStorageKey());
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(deliverable.getContentType() != null
                        ? deliverable.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + deliverable.getFileName() + "\"")
                .body(resource);
    }
}
