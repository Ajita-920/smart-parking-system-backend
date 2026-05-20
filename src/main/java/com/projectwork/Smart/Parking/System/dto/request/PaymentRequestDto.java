package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequestDto {

    @NotNull(message = "Booking ID is required.")
    private UUID bookingId;

    @NotBlank(message = "Payment method is required.")
    @Pattern(regexp = "^(?i)(CASH|KHALTI|ESEWA)$", message = "Payment method must be one of: CASH, KHALTI, ESEWA.")
    private String paymentMethod;
}