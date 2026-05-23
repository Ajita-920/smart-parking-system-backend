package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Payment;

import java.util.UUID;

/**
 * Payment use cases and low-level payment lookups.
 */
public interface PaymentService {

    /**
     * Creates a Khalti payment initiation request.
     */
    PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request);

    /**
     * Verifies a Khalti transaction by pidx and updates local payment state.
     */
    PaymentResponseDto verifyKhaltiPayment(String pidx);

    /**
     * Persists or processes an existing Payment entity.
     */
    Payment processPayment(Payment payment);

    /**
     * Finds a payment by id.
     */
    Payment getPaymentById(UUID id);
}
