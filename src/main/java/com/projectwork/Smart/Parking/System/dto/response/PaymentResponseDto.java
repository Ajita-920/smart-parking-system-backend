package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    private Long paymentId;
    private Long bookingId;
    private double amount;
    private String status;
    private String transactionId;
    private LocalDateTime paidAt;
}
