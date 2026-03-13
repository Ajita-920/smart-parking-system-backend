package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/khalti")
@CrossOrigin(origins = "*")
public class PaymentCallbackController {

    @Autowired
    private PaymentService paymentService;


    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(
            @RequestParam String pidx) {

        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);

        return ResponseEntity.ok(
                new ApiResponse("Payment verification completed", response)
        );
    }
}