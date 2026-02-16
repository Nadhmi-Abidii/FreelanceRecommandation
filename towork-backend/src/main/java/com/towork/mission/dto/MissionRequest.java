package com.towork.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.towork.mission.entity.BudgetType;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;
import com.towork.user.entity.Client;
import com.towork.user.entity.Domaine;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionRequest {
    
    @NotNull(message = "Client ID is required")
    private Long clientId;
    
    @NotNull(message = "Domaine ID is required")
    private Long domaineId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String requirements;
    
    private BigDecimal budgetMin;
    
    private BigDecimal budgetMax;
    
    @NotNull(message = "Budget type is required")
    private BudgetType budgetType;
    
    @NotNull(message = "Type travail is required")
    private TypeTravail typeTravail;
    
    @NotNull(message = "Niveau experience is required")
    private NiveauExperience niveauExperience;
    
    private LocalDate deadline;
    
    private Integer estimatedDuration;
    
    private String skillsRequired;
    
    private Boolean isUrgent = false;
    
    private String attachments;
}
