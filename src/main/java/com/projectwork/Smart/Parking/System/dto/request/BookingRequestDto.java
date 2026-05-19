package com.projectwork.Smart.Parking.System.dto.request;

import com.projectwork.Smart.Parking.System.entity.VehicleType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequestDto {
    @NotNull
    private Long parkingLocationId;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @NotNull
    private VehicleType vehicleType;
}
