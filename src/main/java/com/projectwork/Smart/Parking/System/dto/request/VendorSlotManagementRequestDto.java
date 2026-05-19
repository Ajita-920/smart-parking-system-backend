package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorSlotManagementRequestDto {
    @NotNull
    private Integer slotCount;

    @NotNull
    private Boolean allocate;
}
