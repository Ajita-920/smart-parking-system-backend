package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

@Data
public class ParkingSlotResponseDto {
    private Long id;
    private Long parkingLocationId;
    private String slotNumber;
    private String status;
}
