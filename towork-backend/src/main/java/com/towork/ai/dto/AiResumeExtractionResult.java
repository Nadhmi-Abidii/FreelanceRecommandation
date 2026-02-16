package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResumeExtractionResult {
    private String summary;
    private List<AiSkillDto> skills;
}
