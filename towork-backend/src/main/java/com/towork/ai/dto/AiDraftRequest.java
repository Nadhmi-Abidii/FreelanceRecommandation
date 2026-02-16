package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDraftRequest {
    private String title;
    private String description;
    private String requirements;
    private String skillsRequired;
    private String tone;
    private String language;
    private Integer maxLength;
}
