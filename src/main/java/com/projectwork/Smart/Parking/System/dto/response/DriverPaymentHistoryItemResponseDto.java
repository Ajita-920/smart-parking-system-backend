package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class DriverPaymentHistoryItemResponseDto {
    private UUID paymentId;
    private UUID bookingId;
    private String transactionId;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String paymentUrl;
    private String pidx;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String message;
    private DriverPaymentBookingResponseDto booking;
    private DriverPaymentRefundResponseDto refund;
}
