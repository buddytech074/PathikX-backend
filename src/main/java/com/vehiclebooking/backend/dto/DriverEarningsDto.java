package com.vehiclebooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverEarningsDto {
    private BigDecimal dailyEarnings;
    private BigDecimal monthlyEarnings;
    private BigDecimal walletBalance;
    private String period; // "TODAY" or "THIS_MONTH"
}
