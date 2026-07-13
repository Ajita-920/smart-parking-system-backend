package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class AdminPaymentDetailResponseDto {
    private UUID paymentId;
    private UUID bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private String khaltiPidx;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
    private AdminPaymentCustomerResponseDto customer;
    private AdminPaymentBookingResponseDto booking;
    private AdminPaymentParkingResponseDto parking;
    private AdminPaymentRefundResponseDto refund;
}
