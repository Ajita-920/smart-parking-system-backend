package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminPaymentRefundResponseDto {
    private boolean eligible;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;
}
