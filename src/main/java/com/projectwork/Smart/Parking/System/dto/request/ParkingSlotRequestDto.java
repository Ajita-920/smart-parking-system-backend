package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ParkingSlotRequestDto {

    @NotNull(message = "Parking location ID is required.")
    private UUID parkingLocationId;

    @NotBlank(message = "Slot number is required.")
    @Size(max = 30, message = "Slot number must not exceed 30 characters.")
    private String slotNumber;

    @NotBlank(message = "Vehicle type is required.")
    @Pattern(regexp = "^(?i)(TWO_WHEELER|FOUR_WHEELER)$", message = "Vehicle type must be one of: TWO_WHEELER, FOUR_WHEELER.")
    private String vehicleType;
}