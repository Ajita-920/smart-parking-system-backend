package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DriverPaymentRefundResponseDto {
    private boolean refundEligible;
    private RefundStatus refundStatus;
    private BigDecimal refundAmount;
}
