package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateSlotsRequestDto {

    @NotNull(message = "availableSlots is required.")
    @Min(value = 0, message = "availableSlots cannot be negative.")
    private Integer availableSlots;

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }
}