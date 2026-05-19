package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
//new added slot management
@Data
public class VendorSlotManagementRequestDto {
    @NotNull
    private Integer slotCount;

    @NotNull
    private Boolean allocate;
}
