package com.towork.mission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.towork.mission.entity.BudgetType;
import com.towork.mission.entity.MissionStatus;
import com.towork.mission.entity.NiveauExperience;
import com.towork.mission.entity.TypeTravail;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {
    private Long id;
    private Long clientId;
    private Long domaineId;
    private Long assignedFreelancerId;

    private String clientName;
    private String clientCompanyName;
    private String clientCity;
    private String clientCountry;
    private String clientProfilePicture;
    private String domaineName;
    private String assignedFreelancerName;
    private String assignedFreelancerEmail;

    private String title;
    private String description;
    private String requirements;

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BudgetType budgetType;

    private TypeTravail typeTravail;
    private NiveauExperience niveauExperience;
    private MissionStatus status;

    private LocalDate deadline;
    private Integer estimatedDuration; // days
    private String skillsRequired;
    private Boolean isUrgent;
    private String attachments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
