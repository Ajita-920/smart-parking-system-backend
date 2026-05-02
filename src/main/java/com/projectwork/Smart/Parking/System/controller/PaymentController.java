package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController extends BaseController{

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/khalti/initiate")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully", response);
    }
}