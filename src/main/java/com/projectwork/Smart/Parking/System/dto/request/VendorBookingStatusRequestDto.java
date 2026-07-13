package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VendorBookingStatusRequestDto {

    @NotBlank(message = "Action is required.")
    @Pattern(
            regexp = "^(?i)(CHECK_IN|COMPLETE)$",
            message = "Action must be one of: CHECK_IN, COMPLETE."
    )
    private String action;
}
