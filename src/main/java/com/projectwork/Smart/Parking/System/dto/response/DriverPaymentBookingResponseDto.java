package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DriverPaymentBookingResponseDto {
    private UUID bookingId;
    private UUID parkingLocationId;
    private String parkingLocationName;
    private String address;
    private UUID slotId;
    private String slotNumber;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;
    private Double ratePerHour;
    private BigDecimal totalAmount;
    private BookingStatus status;
}
