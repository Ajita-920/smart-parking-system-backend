package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminPaymentSummaryResponseDto {
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long successfulCount;
    private long pendingCount;
    private long failedCount;
}
