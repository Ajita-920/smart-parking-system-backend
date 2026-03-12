package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Payment;

public interface PaymentService {

        PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request);

        PaymentResponseDto verifyKhaltiPayment(String pidx);

        Payment processPayment(Payment payment);
}