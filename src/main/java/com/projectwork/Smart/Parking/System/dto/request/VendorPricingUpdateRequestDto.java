package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class VendorPricingUpdateRequestDto {
    @DecimalMin(value = "0.0", inclusive = false)
    private Double twoWheelerRatePerHour;

    @DecimalMin(value = "0.0", inclusive = false)
    private Double fourWheelerRatePerHour;
}
