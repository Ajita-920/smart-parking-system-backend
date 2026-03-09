package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private String parkingLocationName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalAmount;
    private String message;
}
