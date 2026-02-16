package com.towork.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSkillDto {
    private String name;
    private String level;
    private Integer yearsOfExperience;
    private Boolean isCertified;
    private String certificationName;
}
