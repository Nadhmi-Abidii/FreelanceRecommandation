package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResumeExtractionResponse {
    private String fileKey;
    private String summary;
    private List<AiSkillDto> skills;
    private Integer createdCount;
}
