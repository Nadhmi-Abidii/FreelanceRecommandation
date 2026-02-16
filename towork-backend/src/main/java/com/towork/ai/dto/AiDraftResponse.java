package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDraftResponse {
    private String title;
    private String description;
    private String requirements;
    private String skillsSuggested;
    private String notes;
}
