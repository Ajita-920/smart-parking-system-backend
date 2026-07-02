package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DriverPaymentSummaryResponseDto {
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private long totalPaymentCount;
    private long successfulPaymentCount;
    private BigDecimal successfulAmount = BigDecimal.ZERO;
    private long pendingPaymentCount;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private long failedPaymentCount;
    private BigDecimal failedAmount = BigDecimal.ZERO;
}
