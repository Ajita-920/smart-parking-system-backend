package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalkInBookingRequestDto {

    @NotNull(message = "Parking location ID is required.")
    private UUID parkingLocationId;

    @NotNull(message = "Parking slot ID is required.")
    private UUID slotId;

    @NotBlank(message = "Vehicle type is required.")
    @Pattern(regexp = "^(?i)(TWO_WHEELER|FOUR_WHEELER)$", message = "Vehicle type must be one of: TWO_WHEELER, FOUR_WHEELER.")
    private String vehicleType;

    @NotBlank(message = "Customer name is required.")
    @Size(max = 100, message = "Customer name must not exceed 100 characters.")
    private String customerName;

    @NotBlank(message = "Customer phone is required.")
    @Size(max = 20, message = "Customer phone must not exceed 20 characters.")
    private String customerPhone;

    @NotBlank(message = "Vehicle number is required.")
    @Size(max = 30, message = "Vehicle number must not exceed 30 characters.")
    private String vehicleNumber;

    @NotNull(message = "Start time is required.")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    private LocalDateTime endTime;

    @NotBlank(message = "Payment method is required.")
    @Pattern(regexp = "^(?i)(CASH|KHALTI|ESEWA)$", message = "Payment method must be one of: CASH, KHALTI, ESEWA.")
    private String paymentMethod;

    @AssertTrue(message = "End time must be after start time.")
    private boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;
        }

        return endTime.isAfter(startTime);
    }
}
