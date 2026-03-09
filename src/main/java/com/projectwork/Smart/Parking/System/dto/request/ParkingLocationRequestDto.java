package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParkingLocationRequestDto {
    @NotBlank
    private String name;
    private String address;
    @NotNull
    private double latitude;
    @NotNull
    private double longitude;
    private int totalSlots;
    private int availableSlots;
}
