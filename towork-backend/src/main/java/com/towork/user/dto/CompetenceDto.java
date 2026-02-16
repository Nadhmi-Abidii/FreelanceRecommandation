package com.towork.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetenceDto {
    private Long id;
    private Long freelancerId;
    private String freelancerName;
    private String name;
    private String description;
    private String level;
    private Integer yearsOfExperience;
    private Boolean isCertified;
    private String certificationName;
    private LocalDate certificationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
