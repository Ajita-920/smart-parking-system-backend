package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Booking ID is required.")
    private Long bookingId;

    @NotBlank(message = "Payment method is required.")
    private String paymentMethod;  // e.g. "KHALTI"
}