package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private String parkingName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalAmount;
    //new refund amount and vehicle type added
    private VehicleType vehicleType;
    private Double refundAmount;
    private String message;
}
