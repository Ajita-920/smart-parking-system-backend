package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class AdminPaymentParkingResponseDto {
    private UUID id;
    private String name;
    private UUID slotId;
    private String slotCode;
    private UUID vendorId;
    private String vendorName;
}
