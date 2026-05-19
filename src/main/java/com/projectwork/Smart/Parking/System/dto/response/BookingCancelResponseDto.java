package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

@Data
public class BookingCancelResponseDto {
    private Long bookingId;
    private String status;
    private boolean refunded;
    private double refundAmount;
    private String message;
}
