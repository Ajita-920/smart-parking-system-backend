package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    private final RestClient restClient = RestClient.create();

    @Value("${khalti.initiate.url}")
    private String khaltiInitiateUrl;

    @Value("${khalti.verify.url}")
    private String khaltiVerifyUrl;

    @Value("${khalti.secret.key}")
    private String khaltiSecretKey;

    /**
     * The URL Khalti redirects back to after the user completes payment.
     * Configured in application.properties so it works across environments
     * (localhost dev, staging, production) without touching code.
     *
     * Example in application.properties:
     * khalti.return.url=http://localhost:8080/api/payments/khalti/verify
     */
    @Value("${khalti.return.url}")
    private String khaltiReturnUrl;

    @Value("${app.website.url}")
    private String websiteUrl;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found."));

        double amount = booking.getTotalAmount(); // use actual booking amount, not hardcoded

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaymentMethod("KHALTI");

        Payment savedPayment = paymentRepository.save(payment);

        // Khalti expects amount in paisa (1 NPR = 100 paisa)
        Map<String, Object> requestBody = Map.of(
                "return_url", khaltiReturnUrl,
                "website_url", websiteUrl,
                "amount", (int) (amount * 100),
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No response received from Khalti.");
        }

        String paymentUrl = (String) khaltiResponse.get("payment_url");
        String pidx = (String) khaltiResponse.get("pidx");

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(savedPayment.getId());
        dto.setBookingId(booking.getId());
        dto.setAmount(amount);
        dto.setStatus("PENDING");
        dto.setTransactionId(savedPayment.getTransactionId());
        dto.setPaidAt(LocalDateTime.now());
        dto.setPaymentUrl(paymentUrl);
        dto.setPidx(pidx);
        dto.setMessage("Redirect the user to paymentUrl to complete payment.");

        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No response received from Khalti during verification.");
        }

        String status = (String) khaltiResponse.get("status");

        // Update Payment record status if we find it by pidx
        // (pidx isn't stored yet — consider adding a pidx column to Payment entity)

        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setStatus(status);
        dto.setMessage("Payment status: " + status);
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Payment processPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment with ID " + id + " not found."));
    }
}