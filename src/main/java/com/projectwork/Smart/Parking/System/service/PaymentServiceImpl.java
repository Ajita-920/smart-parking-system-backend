package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private final RestClient restClient = RestClient.create();

    private static final String KHALTI_INITIATE_URL =
            "https://dev.khalti.com/api/v2/epayment/initiate/";

    private static final String KHALTI_VERIFY_URL =
            "https://dev.khalti.com/api/v2/epayment/lookup/";

    private static final String KHALTI_SECRET_KEY =
            "test_secret_key_xxxxxxxxxxxxxx";

    // payment initiation

    @Override
    @Transactional
    public PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        double amount = 100.0;

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0,8));
        payment.setPaymentMethod("KHALTI");

        Payment savedPayment = paymentRepository.save(payment);

        // request body

        Map<String,Object> body = new HashMap<>();
        body.put("return_url","http://localhost:8080/api/payment/khalti/verify");
        body.put("website_url","http://localhost:8080");
        body.put("amount",(int)(amount * 100));
        body.put("purchase_order_id",savedPayment.getTransactionId());
        body.put("purchase_order_name","Parking Booking");

        Map response = restClient.post()
                .uri(KHALTI_INITIATE_URL)
                .header("Authorization","Key " + KHALTI_SECRET_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String paymentUrl = (String) response.get("payment_url");
        String pidx = (String) response.get("pidx");

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(savedPayment.getId());
        dto.setBookingId(booking.getId());
        dto.setAmount(amount);
        dto.setStatus("PENDING");
        dto.setTransactionId(savedPayment.getTransactionId());
        dto.setPaidAt(LocalDateTime.now());
        dto.setPaymentUrl(paymentUrl);
        dto.setPidx(pidx);
        dto.setMessage("Khalti payment initiated successfully");

        return dto;
    }

    // verify

    @Override
    public PaymentResponseDto verifyKhaltiPayment(String pidx) {

        Map<String,String> body = new HashMap<>();
        body.put("pidx",pidx);

        Map response = restClient.post()
                .uri(KHALTI_VERIFY_URL)
                .header("Authorization","Key " + KHALTI_SECRET_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        String status = (String) response.get("status");

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setStatus(status);
        dto.setMessage("Payment verification completed");

        return dto;
    }

    // save

    @Override
    public Payment processPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return null;
    }
}