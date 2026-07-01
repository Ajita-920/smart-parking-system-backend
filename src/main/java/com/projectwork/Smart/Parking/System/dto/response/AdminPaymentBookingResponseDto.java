package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AdminPaymentBookingResponseDto {
    private UUID id;
    private BookingStatus status;
    private String source;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;
    private VehicleType vehicleType;
    private String vehicleNumber;
}
