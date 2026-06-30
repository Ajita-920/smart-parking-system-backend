package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SlotStatusUpdateRequestDto {

    @NotBlank(message = "Status is required.")
    @Pattern(
            regexp = "^(?i)(AVAILABLE|MAINTENANCE)$",
            message = "Status must be one of: AVAILABLE, MAINTENANCE."
    )
    private String status;
}
