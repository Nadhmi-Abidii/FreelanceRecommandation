package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationResponse {
    private Boolean flagged;
    private Double score;
    private String label;
    private String reason;
}
