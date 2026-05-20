package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingResponseDto {

    private UUID bookingId;

    private UUID driverId;
    private String driverName;

    private UUID parkingLocationId;
    private String parkingLocationName;

    private UUID slotId;
    private String slotNumber;

    private VehicleType vehicleType;

    private BookingStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Instant cancelledAt;

    private BigDecimal totalAmount;

    private String message;
}