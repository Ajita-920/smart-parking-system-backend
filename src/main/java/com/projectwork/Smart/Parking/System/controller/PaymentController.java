package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.service.PaymentService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    public Payment pay(@RequestBody Payment payment){
        return paymentService.processPayment(payment);
    }
}
