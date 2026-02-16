package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiFreelancerMatchDto {
    private Long freelancerId;
    private String freelancerName;
    private String title;
    private Double score;
    private String reason;
}
