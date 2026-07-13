package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingRequestDto {

    @NotNull(message = "Parking location ID is required.")
    private UUID parkingLocationId;

    @NotNull(message = "Parking slot ID is required.")
    private UUID slotId;

    @NotBlank(message = "Vehicle type is required.")
    @Pattern(regexp = "^(?i)(TWO_WHEELER|FOUR_WHEELER)$", message = "Vehicle type must be one of: TWO_WHEELER, FOUR_WHEELER.")
    private String vehicleType;

    @NotBlank(message = "Vehicle number is required.")
    private String vehicleNumber;

    @NotNull(message = "Start time is required.")
    @Future(message = "Start time must be in the future.")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    @Future(message = "End time must be in the future.")
    private LocalDateTime endTime;

    @AssertTrue(message = "End time must be after start time.")
    private boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;
        }

        return endTime.isAfter(startTime);
    }
}
