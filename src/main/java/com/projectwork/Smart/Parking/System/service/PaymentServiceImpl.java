package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        double amount = 100.0; // You can make this dynamic later (per hour × duration)

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
        payment.setPaymentMethod(request.getPaymentMethod().toUpperCase());

        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(savedPayment.getId());
        response.setBookingId(booking.getId());
        response.setAmount(amount);
        response.setStatus("PENDING");
        response.setTransactionId(savedPayment.getTransactionId());
        response.setPaidAt(LocalDateTime.now());
        response.setMessage("Payment initiated successfully");

        // ==================== ESEWA INTEGRATION ====================
        if ("ESEWA".equalsIgnoreCase(request.getPaymentMethod())) {
            String esewaTestUrl = "https://uat.esewa.com.np/epay/main?" +
                    "amt=" + amount +
                    "&psc=0& pdc=0&txAmt=0&tAmt=" + amount +
                    "&pid=" + savedPayment.getTransactionId() +
                    "&scd=EPAYTEST" +   // Test Merchant Code
                    "&su=http://localhost:8080/api/payment/esewa/success" +
                    "&fu=http://localhost:8080/api/payment/esewa/failure";

            response.setEsewaPaymentUrl(esewaTestUrl);
        }

        return response;
    }

    @Override
    public Payment processPayment(Payment payment) {
        return null;
    }
}