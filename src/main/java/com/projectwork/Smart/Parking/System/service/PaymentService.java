package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;


    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment proceePayment(Payment payment){

        if(payment.getMethod().equals("CASH")){
            payment.setStatus("PENDING");
        } else{
            payment.setStatus("PAID");
        }
        return payment;
    }

    public Payment processPayment(Payment payment) {
        return payment;
    }
}
