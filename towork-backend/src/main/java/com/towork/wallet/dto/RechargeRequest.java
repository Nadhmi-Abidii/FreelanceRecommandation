package com.towork.wallet.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {
    private BigDecimal amount;
    private String description;
}
