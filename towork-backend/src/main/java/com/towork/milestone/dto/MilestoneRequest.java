package com.towork.milestone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.towork.mission.entity.Mission;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneRequest {
    
    @NotNull(message = "Mission ID is required")
    private Long missionId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private LocalDate dueDate;
    
    @NotNull(message = "Order index is required")
    private Integer orderIndex;
}
