package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.PAYMENT_KHALTI_BASE)
@CrossOrigin(origins = "*")
public class PaymentCallbackController extends BaseController {

    @Autowired
    private PaymentService paymentService;


    @GetMapping(ApiConstant.PAYMENT_KHALTI_VERIFY)
    public ResponseEntity<ApiResponse<PaymentResponseDto>> verifyPayment(
            @RequestParam String pidx) {
        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        return okResponse("Payment verification completed", response);
    }
}
