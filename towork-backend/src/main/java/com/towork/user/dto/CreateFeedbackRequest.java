package com.towork.user.dto;

import com.towork.user.entity.FeedbackDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(min = 10, max = 2000)
    private String comment;

    /**
     * Optional. Defaults to CLIENT_TO_FREELANCER for clients and FREELANCER_TO_CLIENT for freelancers.
     */
    private FeedbackDirection direction;
}
