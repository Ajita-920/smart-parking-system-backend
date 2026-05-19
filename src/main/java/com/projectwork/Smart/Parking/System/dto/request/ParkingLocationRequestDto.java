package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParkingLocationRequestDto {

    @NotBlank(message = "Parking location name is required.")
    private String name;

    private String address;

    /**
     * @NotNull on a primitive double does nothing — primitives can never be null.
     * Use Double (wrapper) + @NotNull, or keep double + range constraints.
     * Latitude range: -90 to +90.
     */
    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.")
    @DecimalMax(value = "90.0",  message = "Latitude must be <= 90.")
    private Double latitude;

    /**
     * Longitude range: -180 to +180.
     */
    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180.")
    private Double longitude;

    @Min(value = 1, message = "Total slots must be at least 1.")
    private int totalSlots;
}