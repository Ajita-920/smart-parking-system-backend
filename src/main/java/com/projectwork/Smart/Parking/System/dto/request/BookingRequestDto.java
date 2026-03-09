package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {

    @NotNull
    private Long ParkingSlotId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
