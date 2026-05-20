package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;

import java.util.UUID;

@Data
public class ParkingSlotResponseDto {

    private UUID id;
    private UUID parkingLocationId;
    private String parkingLocationName;

    private String slotNumber;
    private VehicleType vehicleType;
    private ParkingSlotStatus status;
}