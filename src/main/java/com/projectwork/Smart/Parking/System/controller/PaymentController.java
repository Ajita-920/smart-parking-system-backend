package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.PAYMENT_BASE)
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(ApiConstant.PAYMENT_KHALTI_INITIATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @Valid @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully!", response);
    }

    @GetMapping(ApiConstant.PAYMENT_KHALTI_VERIFY)
    public ResponseEntity<ApiResponse<PaymentResponseDto>> verifyKhaltiPayment(
            @RequestParam String pidx) {

        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        return okResponse("Payment verification completed!", response);
    }
}