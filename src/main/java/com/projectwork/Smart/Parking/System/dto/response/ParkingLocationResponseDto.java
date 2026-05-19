package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

@Data
public class ParkingLocationResponseDto {
    private Long id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int availableSlots;
    private int totalSlots;
    private Double twoWheelerRatePerHour;
    private Double fourWheelerRatePerHour;
    private double distance;
    private String vendorName;
}
