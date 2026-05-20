package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class ParkingLocationResponseDto {

    private UUID id;

    private String name;
    private String address;

    private Double latitude;
    private Double longitude;

    private Integer totalFourWheelerSlots;
    private Integer availableFourWheelerSlots;

    private Integer totalTwoWheelerSlots;
    private Integer availableTwoWheelerSlots;

    private Integer totalSlots;
    private Integer availableSlots;

    private Double twoWheelerRatePerHour;
    private Double fourWheelerRatePerHour;

    private Double distance;

    private UUID vendorId;
    private String vendorName;
}