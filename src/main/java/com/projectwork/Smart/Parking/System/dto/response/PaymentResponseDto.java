package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentResponseDto {

    private UUID paymentId;
    private UUID bookingId;

    private BigDecimal amount;

    private PaymentStatus status;
    private PaymentMethod paymentMethod;

    private String transactionId;
    private String paymentUrl;

    private Instant paidAt;

    private String message;

    private String pidx;
}