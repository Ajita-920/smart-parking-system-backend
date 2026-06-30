package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Integrates booking payments with Khalti and keeps local payment records in
 * sync with verification results.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RestClient restClient;

    @Value("${khalti.initiate.url}")
    private String khaltiInitiateUrl;

    @Value("${khalti.verify.url}")
    private String khaltiVerifyUrl;

    @Value("${khalti.secret.key}")
    private String khaltiSecretKey;

    @Value("${khalti.return.url}")
    private String khaltiReturnUrl;

    @Value("${app.website.url}")
    private String websiteUrl;

    public PaymentServiceImpl(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.restClient = RestClient.create();
    }

    @Override
    @Transactional
    public PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request) {

        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        System.out.println("Parsing payment method (P1): " + paymentMethod);
        if (paymentMethod != PaymentMethod.KHALTI) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint only supports KHALTI payment.");
        }

        System.out.println("Fetching booking for ID (B1): " + request.getBookingId());
        Booking booking = bookingRepository.findByIdAndDeletedAtIsNull(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found."));

        BigDecimal amount = booking.getTotalAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid booking amount.");
        }

        System.out.println("Creating payment record for booking (P2): " + booking.getId() + " with amount: " + amount);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.KHALTI);
        payment.setTransactionId(generateTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        int amountInPaisa = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        Map<String, Object> requestBody = Map.of(
                "return_url", khaltiReturnUrl,
                "website_url", websiteUrl,
                "amount", amountInPaisa,
                "purchase_order_id", savedPayment.getTransactionId(),
                "purchase_order_name", "Parking Booking #" + booking.getId());

        Map<String, Object> khaltiResponse = restClient.post()
                .uri(khaltiInitiateUrl)
                .header("Authorization", khaltiSecretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (khaltiResponse == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No response received from Khalti.");
        }

        String paymentUrl = (String) khaltiResponse.get("payment_url");
        String pidx = (String) khaltiResponse.get("pidx");

        System.out.println("Retrieved payment URL (P3): " + paymentUrl);
        System.out.println("Retrieved pidx (P4): " + pidx);

        savedPayment.setPaymentUrl(paymentUrl);
        savedPayment.setPidx(pidx);

        Payment updatedPayment = paymentRepository.save(savedPayment);

        PaymentResponseDto response = mapToResponse(updatedPayment);
        System.out.println("Returning payment response (R2): " + response);
        response.setPidx(pidx);
        response.setMessage("Redirect the user to paymentUrl to complete payment.");

        return response;
    }

    @Override
    @Transactional
    public PaymentResponseDto verifyKhaltiPayment(String pidx) {

        Map<String, String> requestBody = Map.of("pidx", pidx);

        Map<String, Object> khaltiResponse = restClient.post()
                .uri(khaltiVerifyUrl)
                .header("Authorization", khaltiSecretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (khaltiResponse == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No response received from Khalti during verification.");
        }

        Payment payment = paymentRepository.findByPidxAndDeletedAtIsNull(pidx)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found for this pidx."));

        String khaltiStatus = String.valueOf(khaltiResponse.get("status"));

        if ("Completed".equalsIgnoreCase(khaltiStatus)
                || "SUCCESS".equalsIgnoreCase(khaltiStatus)) {
            payment.markSuccess();
        } else if ("Refunded".equalsIgnoreCase(khaltiStatus)) {
            payment.markRefundCompleted();
        } else if ("Failed".equalsIgnoreCase(khaltiStatus)
                || "Expired".equalsIgnoreCase(khaltiStatus)
                || "User canceled".equalsIgnoreCase(khaltiStatus)) {
            payment.markFailed();
        }

        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponseDto response = mapToResponse(savedPayment);
        response.setPidx(pidx);
        response.setMessage("Payment status: " + khaltiStatus);

        return response;
    }

    @Override
    @Transactional
    public Payment processPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found."));
    }

    private PaymentResponseDto mapToResponse(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setPaymentId(payment.getId());
        dto.setBookingId(payment.getBooking().getId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaymentUrl(payment.getPaymentUrl());
        dto.setPaidAt(payment.getPaidAt());
        dto.setPidx(payment.getPidx());

        return dto;
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment method. Allowed values: CASH, KHALTI, ESEWA.");
        }
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
