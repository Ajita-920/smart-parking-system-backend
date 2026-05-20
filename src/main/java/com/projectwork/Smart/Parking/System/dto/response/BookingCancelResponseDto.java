package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BookingCancelResponseDto {

    private UUID bookingId;
    private String status;

    private boolean refunded;
    private String refundStatus;
    private BigDecimal refundAmount;

    private String message;
}