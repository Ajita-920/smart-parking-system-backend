package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Integrates booking payments with Khalti and keeps local payment records in
 * sync with verification results.
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final ParkingLocationRepository parkingLocationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
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
            ParkingLocationRepository parkingLocationRepository,
            ParkingSlotRepository parkingSlotRepository,
            PaymentRepository paymentRepository,
            EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.parkingLocationRepository = parkingLocationRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.restClient = RestClient.create();
    }

    @Override
    @Transactional
    public PaymentResponseDto initiateKhaltiPayment(PaymentRequestDto request) {

        PaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        if (paymentMethod != PaymentMethod.KHALTI) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint only supports KHALTI payment.");
        }

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

        Optional<Payment> successfulPayment = paymentRepository
                .findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                        booking.getId(),
                        PaymentStatus.SUCCESS);

        if (successfulPayment.isPresent()) {
            PaymentResponseDto response = mapToResponse(successfulPayment.get());
            response.setMessage("Payment already completed.");
            return response;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment can only be initiated for pending bookings.");
        }

        Optional<Payment> latestPayment = paymentRepository
                .findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(booking.getId());

        if (latestPayment.isPresent()) {
            Payment existingPayment = latestPayment.get();
            if (existingPayment.getPaymentMethod() == PaymentMethod.KHALTI
                    && existingPayment.getStatus() == PaymentStatus.PENDING
                    && existingPayment.getPaymentUrl() != null
                    && existingPayment.getPidx() != null) {
                PaymentResponseDto response = mapToResponse(existingPayment);
                response.setMessage("Redirect the user to paymentUrl to complete payment.");
                return response;
            }
        }

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

        savedPayment.setPaymentUrl(paymentUrl);
        savedPayment.setPidx(pidx);

        Payment updatedPayment = paymentRepository.save(savedPayment);

        PaymentResponseDto response = mapToResponse(updatedPayment);
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
            extractKhaltiPaidAmount(khaltiResponse).ifPresent(payment::setAmount);
            payment.markSuccess();
            Booking booking = payment.getBooking();
            if (booking != null) {
                if (booking.getStatus() != BookingStatus.PENDING) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Payment can only confirm pending bookings.");
                }

                booking.setStatus(BookingStatus.CONFIRMED);
                ParkingSlot slot = booking.getSlot();
                if (!booking.isWalkIn()
                        && slot != null
                        && slot.getStatus() == ParkingSlotStatus.RESERVED) {
                    slot.markBooked();
                    parkingSlotRepository.save(slot);
                }
                Booking savedBooking = bookingRepository.save(booking);
                dispatchBookingConfirmation(savedBooking);
            }
        } else if ("Refunded".equalsIgnoreCase(khaltiStatus)) {
            payment.markRefundCompleted();
        } else if ("Failed".equalsIgnoreCase(khaltiStatus)
                || "Expired".equalsIgnoreCase(khaltiStatus)
                || "User canceled".equalsIgnoreCase(khaltiStatus)) {
            payment.markFailed();
            Booking booking = payment.getBooking();
            if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                booking.markCancelled();
                releaseSlotAndIncreaseAvailability(booking);
                bookingRepository.save(booking);
            }
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

    private Optional<BigDecimal> extractKhaltiPaidAmount(Map<String, Object> khaltiResponse) {
        return extractPaisaAmount(khaltiResponse, "total_amount")
                .or(() -> extractPaisaAmount(khaltiResponse, "paid_amount"))
                .or(() -> extractPaisaAmount(khaltiResponse, "amount"));
    }

    private Optional<BigDecimal> extractPaisaAmount(Map<String, Object> khaltiResponse, String key) {
        Object rawAmount = khaltiResponse.get(key);

        if (rawAmount == null) {
            return Optional.empty();
        }

        try {
            BigDecimal paisaAmount = new BigDecimal(String.valueOf(rawAmount));
            if (paisaAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Optional.empty();
            }

            return Optional.of(paisaAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        } catch (NumberFormatException ex) {
            log.warn("Ignoring non-numeric Khalti amount field {}={}", key, rawAmount);
            return Optional.empty();
        }
    }

    private void releaseSlotAndIncreaseAvailability(Booking booking) {
        ParkingSlot slot = booking.getSlot();

        if (slot != null && slot.getStatus() == ParkingSlotStatus.RESERVED) {
            slot.markAvailable();
            parkingSlotRepository.save(slot);
        }

        ParkingLocation location = booking.getParkingLocation();

        if (location == null || slot == null) {
            return;
        }

        if (slot.getVehicleType() == null) {
            return;
        }

        if (slot.getVehicleType() == VehicleType.FOUR_WHEELER) {
            int current = location.getAvailableFourWheelerSlots();
            int total = location.getTotalFourWheelerSlots();
            location.setAvailableFourWheelerSlots(Math.min(total, current + 1));
        } else {
            int current = location.getAvailableTwoWheelerSlots();
            int total = location.getTotalTwoWheelerSlots();
            location.setAvailableTwoWheelerSlots(Math.min(total, current + 1));
        }

        parkingLocationRepository.save(location);
    }

    private void dispatchBookingConfirmation(Booking booking) {
        if (booking.getDriver() == null || booking.getDriver().getEmail() == null) {
            return;
        }

        try {
            emailService.sendBookingConfirmation(mapBookingToResponse(booking), booking.getDriver().getEmail());
        } catch (Exception e) {
            log.error("Could not trigger booking confirmation email for booking {}", booking.getId(), e);
        }
    }

    private BookingResponseDto mapBookingToResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingId(booking.getId());

        if (booking.getDriver() != null) {
            dto.setDriverId(booking.getDriver().getId());
            dto.setDriverName(booking.getDriver().getName());
        }

        dto.setCustomerName(booking.getCustomerName());
        dto.setCustomerPhone(booking.getCustomerPhone());
        dto.setVehicleNumber(booking.getVehicleNumber());
        dto.setWalkIn(booking.isWalkIn());

        if (booking.getParkingLocation() != null) {
            dto.setParkingLocationId(booking.getParkingLocation().getId());
            dto.setParkingLocationName(booking.getParkingLocation().getName());
            if (booking.getParkingLocation().getVendor() != null) {
                dto.setVendorId(booking.getParkingLocation().getVendor().getId());
                dto.setVendorName(booking.getParkingLocation().getVendor().getName());
            }
        }

        if (booking.getSlot() != null) {
            dto.setSlotId(booking.getSlot().getId());
            dto.setSlotNumber(booking.getSlot().getSlotNumber());
            dto.setSlotStatus(booking.getSlot().getStatus());
            dto.setVehicleType(booking.getSlot().getVehicleType());
        }

        if (booking.getVehicleType() != null) {
            dto.setVehicleType(booking.getVehicleType());
        }

        dto.setStatus(booking.getStatus());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setCancelledAt(booking.getCancelledAt());
        dto.setTotalAmount(booking.getTotalAmount());
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
