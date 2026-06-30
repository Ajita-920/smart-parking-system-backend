package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ParkingSlotResponseDto {

    private UUID id;
    private UUID parkingLocationId;
    private String parkingLocationName;

    private String slotNumber;
    private VehicleType vehicleType;
    private ParkingSlotStatus status;
    private ActiveBookingSummaryDto activeBooking;

    @Data
    public static class ActiveBookingSummaryDto {
        private UUID bookingId;
        private String driverName;
        private String customerName;
        private String customerPhone;
        private String vehicleNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BookingStatus status;
    }
}
