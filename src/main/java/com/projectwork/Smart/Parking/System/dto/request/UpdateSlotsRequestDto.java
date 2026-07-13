package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSlotsRequestDto {

    @NotNull(message = "Available four-wheeler slots is required.")
    @Min(value = 0, message = "Available four-wheeler slots cannot be negative.")
    private Integer availableFourWheelerSlots;

    @NotNull(message = "Available two-wheeler slots is required.")
    @Min(value = 0, message = "Available two-wheeler slots cannot be negative.")
    private Integer availableTwoWheelerSlots;
}