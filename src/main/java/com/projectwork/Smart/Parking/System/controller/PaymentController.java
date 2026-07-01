package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Payment endpoints for initiating and verifying Khalti transactions.
 */
@RestController
@RequestMapping(ApiConstant.PAYMENT_BASE)
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    @Value("${app.website.url}")
    private String websiteUrl;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Starts a Khalti payment flow for an authenticated user.
     */
    @PostMapping(ApiConstant.PAYMENT_KHALTI_INITIATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @Valid @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully!", response);
    }

    /**
     * Verifies a Khalti payment callback or redirect by pidx.
     */
    @GetMapping(ApiConstant.PAYMENT_KHALTI_VERIFY)
    public ResponseEntity<?> verifyKhaltiPayment(
            @RequestParam String pidx,
            HttpServletRequest request) {

        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        if (shouldRedirectToTicket(request)) {
            URI ticketUri = UriComponentsBuilder
                    .fromUriString(websiteUrl)
                    .path("/bookings")
                    .queryParam("paymentSuccess", true)
                    .queryParam("bookingId", response.getBookingId())
                    .queryParam("paymentId", response.getPaymentId())
                    .queryParam("status", response.getStatus())
                    .queryParam("paymentMethod", response.getPaymentMethod())
                    .queryParam("amount", response.getAmount())
                    .queryParam("transactionId", response.getTransactionId())
                    .queryParam("paidAt", response.getPaidAt())
                    .queryParam("pidx", response.getPidx())
                    .queryParam("message", response.getMessage())
                    .build()
                    .encode()
                    .toUri();

            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(ticketUri)
                    .build();
        }

        return okResponse("Payment verification completed!", response);
    }

    private boolean shouldRedirectToTicket(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return request.getParameter("status") != null
                || (acceptHeader != null && acceptHeader.contains("text/html"));
    }
}
