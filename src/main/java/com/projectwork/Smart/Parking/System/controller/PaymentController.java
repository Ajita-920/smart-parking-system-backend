package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.PAYMENT_BASE)
public class PaymentController extends BaseController {

    @Autowired
    private PaymentService paymentService;

    /**
     * POST /api/payments/khalti/initiate
     * Body: PaymentRequestDto
     * Initiates a Khalti payment and returns the payment URL + pidx.
     * Requires authentication.
     */
    @PostMapping(ApiConstant.PAYMENT_KHALTI_INITIATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully!", response);
    }

    /**
     * GET /api/payments/khalti/verify?pidx=<pidx>
     * Called by Khalti as the redirect/callback after the user completes payment.
     * Public — Khalti hits this endpoint directly, no user JWT present.
     *
     * Query param:
     *   pidx (required) — payment index token returned by Khalti initiation.
     */
    @GetMapping(ApiConstant.PAYMENT_KHALTI_VERIFY)
    public ResponseEntity<ApiResponse<PaymentResponseDto>> verifyKhaltiPayment(
            @RequestParam String pidx) {

        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        return okResponse("Payment verification completed!", response);
    }
}