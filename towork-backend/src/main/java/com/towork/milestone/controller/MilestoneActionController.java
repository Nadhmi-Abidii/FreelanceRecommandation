package com.towork.milestone.controller;

import com.towork.config.MessageResponse;
import com.towork.milestone.dto.MilestoneDto;
import com.towork.milestone.mapper.MilestoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.towork.milestone.entity.Milestone;
import com.towork.milestone.entity.MilestoneDeliverable;
import com.towork.milestone.repository.MilestoneDeliverableRepository;
import com.towork.milestone.service.MilestoneService;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneActionController {

    private final MilestoneService milestoneService;
    private final MilestoneDeliverableRepository deliverableRepository;

    @PostMapping(value = "/{id}/deliverable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> uploadDeliverable(@PathVariable Long id,
                                                             @RequestPart("file") MultipartFile file,
                                                             @RequestPart(value = "comment", required = false) String comment,
                                                             Authentication authentication) {
        UserDetails userDetails = resolveUser(authentication);
        Milestone milestone = milestoneService.uploadDeliverable(id, file, comment, userDetails);
        List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(milestone);
        MilestoneDto dto = MilestoneMapper.toDto(milestone, deliverables, "/api/milestones/deliverables");
        return ResponseEntity.ok(MessageResponse.success("Deliverable uploaded", dto));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> acceptMilestone(@PathVariable Long id,
                                                           @RequestBody(required = false) MilestoneDecisionRequest request,
                                                           Authentication authentication) {
        UserDetails userDetails = resolveUser(authentication);
        String approvalNotes = request != null ? request.approvalNotes() : null;
        Milestone milestone = milestoneService.acceptMilestone(id, approvalNotes, userDetails);
        List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(milestone);
        MilestoneDto dto = MilestoneMapper.toDto(milestone, deliverables, "/api/milestones/deliverables");
        return ResponseEntity.ok(MessageResponse.success("Milestone accepted", dto));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> rejectMilestone(@PathVariable Long id,
                                                           @RequestBody(required = false) MilestoneDecisionRequest request,
                                                           Authentication authentication) {
        UserDetails userDetails = resolveUser(authentication);
        String reason = request != null ? request.reason() : null;
        Milestone milestone = milestoneService.rejectMilestone(id, reason, userDetails);
        List<MilestoneDeliverable> deliverables = deliverableRepository.findByMilestoneOrderByCreatedAtDesc(milestone);
        MilestoneDto dto = MilestoneMapper.toDto(milestone, deliverables, "/api/milestones/deliverables");
        return ResponseEntity.ok(MessageResponse.success("Milestone rejected", dto));
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

    public record MilestoneDecisionRequest(String approvalNotes, String reason) {
    }
}
