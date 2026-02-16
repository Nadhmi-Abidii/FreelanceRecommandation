package com.towork.user.service;

import com.towork.user.dto.CreateFeedbackRequest;
import com.towork.user.dto.FeedbackResponse;
import com.towork.user.dto.FeedbackSummaryResponse;
import com.towork.user.dto.FreelancerFeedbackAggregateResponse;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface FeedbackService {
    FeedbackResponse createFeedback(Long missionId, CreateFeedbackRequest request, UserDetails currentUser);
    FeedbackResponse getMyFeedback(Long missionId, UserDetails currentUser);
    List<FeedbackResponse> getFeedbacksForMission(Long missionId, UserDetails currentUser);
    FeedbackSummaryResponse getFeedbackSummary(Long userId);
    FreelancerFeedbackAggregateResponse getFeedbacksForFreelancer(Long freelancerId);
}
