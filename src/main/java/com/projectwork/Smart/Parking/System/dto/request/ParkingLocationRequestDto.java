package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParkingLocationRequestDto {

    @NotBlank(message = "Parking location name is required.")
    @Size(min = 2, max = 100, message = "Parking location name must be between 2 and 100 characters.")
    private String name;

    @NotBlank(message = "Address is required.")
    @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters.")
    private String address;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.")
    private Double longitude;

    @NotNull(message = "Total four-wheeler slots is required.")
    @Min(value = 0, message = "Total four-wheeler slots cannot be negative.")
    private Integer totalFourWheelerSlots;

    @NotNull(message = "Total two-wheeler slots is required.")
    @Min(value = 0, message = "Total two-wheeler slots cannot be negative.")
    private Integer totalTwoWheelerSlots;

    @DecimalMin(value = "0.0", inclusive = false, message = "Two-wheeler rate must be greater than zero.")
    private Double twoWheelerRatePerHour;

    @DecimalMin(value = "0.0", inclusive = false, message = "Four-wheeler rate must be greater than zero.")
    private Double fourWheelerRatePerHour;
}