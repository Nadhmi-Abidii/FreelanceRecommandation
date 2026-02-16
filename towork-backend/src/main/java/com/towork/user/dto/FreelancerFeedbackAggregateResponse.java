package com.towork.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerFeedbackAggregateResponse {
    private FeedbackSummaryResponse summary;
    private List<FeedbackResponse> feedbacks;
}
