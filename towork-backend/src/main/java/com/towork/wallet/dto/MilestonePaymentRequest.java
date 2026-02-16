package com.towork.wallet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MilestonePaymentRequest {
    @NotNull
    private Long clientId;

    @NotNull
    private Long freelancerId;

    private String paymentMethod = "WALLET";
    private String description;
}
