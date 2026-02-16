package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDomainSuggestion {
    private Long domaineId;
    private String domaineName;
    private Double score;
    private String reason;
}
