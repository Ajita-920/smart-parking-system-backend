package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {

    @NotNull(message = "Parking location ID is required.")
    private Long parkingLocationId;

    @NotNull(message = "Start time is required.")
    @Future(message = "Start time must be in the future.")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    @Future(message = "End time must be in the future.")
    private LocalDateTime endTime;

    /**
     * Cross-field validation: endTime must be strictly after startTime.
     * @AssertTrue runs only when both fields are non-null (nulls are handled
     * by their own @NotNull constraints above).
     */
    @AssertTrue(message = "End time must be after start time.")
    private boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true; // defer to @NotNull
        return endTime.isAfter(startTime);
    }
}