package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDomainSuggestionRequest {
    private String title;
    private String description;
    private String requirements;
    private String skillsRequired;
    private Integer limit;
    private String language;
}
