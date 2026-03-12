package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payment/khalti")
@CrossOrigin(origins = "*")
public class PaymentCallbackController {

    @Autowired
    private PaymentRepository paymentRepository;

    // ==================== ESEWA SUCCESS CALLBACK ====================
    @GetMapping("/success")
    public ResponseEntity<ApiResponse> paymentSuccess(
            @RequestParam String oid,           // transactionId (pid)
            @RequestParam String amt,
            @RequestParam String refId) {

        Payment payment = paymentRepository.findByTransactionId(oid)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // You can also update booking status to PAID here

        return ResponseEntity.ok(new ApiResponse("Payment Successful! Thank you.",
                "Transaction ID: " + oid + " | Amount: Rs. " + amt));
    }

    //FAILURE CALLBACK
    @GetMapping("/failure")
    public ResponseEntity<ApiResponse> paymentFailure(
            @RequestParam String oid,
            @RequestParam String amt) {

        Payment payment = paymentRepository.findByTransactionId(oid)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus("FAILED");
        paymentRepository.save(payment);

        return ResponseEntity.ok(new ApiResponse("Payment Failed. Please try again.", null));
    }
}