package com.projectwork.Smart.Parking.System.dto.request;

import lombok.Data;

@Data
public class PaymentRequestDto {

    private Long bookingID;
    private String paymentMethod;
}
