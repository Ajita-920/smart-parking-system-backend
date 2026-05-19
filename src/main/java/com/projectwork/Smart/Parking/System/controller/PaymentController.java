package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.PAYMENT_BASE)
@CrossOrigin(origins = "*")
public class PaymentController extends BaseController{

    @Autowired
    private PaymentService paymentService;

    @PostMapping(ApiConstant.PAYMENT_KHALTI_INITIATE)
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully", response);
    }

    /**
     * Endpoint: GET /api/payment/khalti/verify
     * Khalti automatically appends query params like ?pidx=xxx&status=Completed to your return_url
     */
    @GetMapping("/khalti" + ApiConstant.PAYMENT_KHALTI_VERIFY) // Evaluates to: /khalti/verify
    public ResponseEntity<ApiResponse<PaymentResponseDto>> verifyKhaltiPayment(
            @RequestParam("pidx") String pidx,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "purchase_order_id", required = false) String purchaseOrderId) {

        // Pass the pidx to your service layer to double-check validation with Khalti's servers
        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        
        return okResponse("Payment verification processed successfully", response);
    }
}
