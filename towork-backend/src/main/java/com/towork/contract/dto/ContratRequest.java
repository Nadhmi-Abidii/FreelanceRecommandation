package com.towork.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.towork.mission.entity.Mission;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratRequest {
    
    @NotNull(message = "Client ID is required")
    private Long clientId;
    
    @NotNull(message = "Freelancer ID is required")
    private Long freelancerId;
    
    @NotNull(message = "Mission ID is required")
    private Long missionId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String terms;
    
    private String paymentTerms;
    
    private Boolean milestoneBased = false;
}
