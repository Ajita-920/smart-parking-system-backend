package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Payment;

public interface PaymentService {

    /**
     * Initiates payment for a booking
     * Supports "ESEWA" and "CASH"
     */
    PaymentResponseDto initiatePayment(PaymentRequestDto request);

    Payment processPayment(Payment payment);
}