package com.towork.user.controller;

import com.towork.config.MessageResponse;
import com.towork.user.dto.CreateFeedbackRequest;
import com.towork.user.dto.FeedbackResponse;
import com.towork.user.dto.FeedbackSummaryResponse;
import com.towork.user.dto.FreelancerFeedbackAggregateResponse;
import com.towork.user.entity.FeedbackDirection;
import com.towork.user.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/missions/{missionId}/feedback")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> createClientFeedback(@PathVariable Long missionId,
                                                                @Valid @RequestBody CreateFeedbackRequest request,
                                                                Authentication authentication) {
        request.setDirection(FeedbackDirection.CLIENT_TO_FREELANCER);
        FeedbackResponse response = feedbackService.createFeedback(missionId, request, resolve(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.success("Feedback created successfully", response));
    }

    @PostMapping("/missions/{missionId}/feedbacks")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER')")
    public ResponseEntity<MessageResponse> createFeedback(@PathVariable Long missionId,
                                                          @Valid @RequestBody CreateFeedbackRequest request,
                                                          Authentication authentication) {
        FeedbackResponse response = feedbackService.createFeedback(missionId, request, resolve(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.success("Feedback created successfully", response));
    }

    @GetMapping("/missions/{missionId}/feedbacks/mine")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<MessageResponse> getMyFeedback(@PathVariable Long missionId,
                                                         Authentication authentication) {
        FeedbackResponse response = feedbackService.getMyFeedback(missionId, resolve(authentication));
        return ResponseEntity.ok(MessageResponse.success("Feedback retrieved successfully", response));
    }

    @GetMapping("/missions/{missionId}/feedbacks")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER','ADMIN')")
    public ResponseEntity<MessageResponse> getFeedbacksForMission(@PathVariable Long missionId,
                                                                  Authentication authentication) {
        List<FeedbackResponse> responses = feedbackService.getFeedbacksForMission(missionId, resolve(authentication));
        return ResponseEntity.ok(MessageResponse.success("Feedbacks retrieved successfully", responses));
    }

    @GetMapping("/users/{userId}/feedbacks/summary")
    public ResponseEntity<MessageResponse> getFeedbackSummary(@PathVariable Long userId) {
        FeedbackSummaryResponse summary = feedbackService.getFeedbackSummary(userId);
        return ResponseEntity.ok(MessageResponse.success("Feedback summary retrieved successfully", summary));
    }

    @GetMapping("/freelances/{freelancerId}/feedbacks")
    public ResponseEntity<MessageResponse> getFreelancerFeedbacks(@PathVariable Long freelancerId) {
        FreelancerFeedbackAggregateResponse payload = feedbackService.getFeedbacksForFreelancer(freelancerId);
        return ResponseEntity.ok(MessageResponse.success("Feedbacks retrieved successfully", payload));
    }

    private UserDetails resolve(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
