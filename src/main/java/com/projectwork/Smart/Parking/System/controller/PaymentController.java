package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.DriverPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.request.PaymentRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentHistoryItemResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentPageResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.service.DriverPaymentService;
import com.projectwork.Smart.Parking.System.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payment endpoints for initiating and verifying Khalti transactions.
 */
@RestController
@RequestMapping(ApiConstant.PAYMENT_BASE)
public class PaymentController extends BaseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentService paymentService;
    private final DriverPaymentService driverPaymentService;

    @Value("${app.website.url}")
    private String websiteUrl;

    public PaymentController(PaymentService paymentService, DriverPaymentService driverPaymentService) {
        this.paymentService = paymentService;
        this.driverPaymentService = driverPaymentService;
    }

    /**
     * Starts a Khalti payment flow for an authenticated user.
     */
    @PostMapping(ApiConstant.PAYMENT_KHALTI_INITIATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> initiateKhaltiPayment(
            @Valid @RequestBody PaymentRequestDto request) {

        PaymentResponseDto response = paymentService.initiateKhaltiPayment(request);
        return okResponse("Khalti payment initiated successfully!", response);
    }

    /**
     * Verifies a Khalti payment callback or redirect by pidx.
     */
    @GetMapping(ApiConstant.PAYMENT_KHALTI_VERIFY)
    public ResponseEntity<?> verifyKhaltiPayment(
            @RequestParam String pidx,
            HttpServletRequest request) {

        PaymentResponseDto response = paymentService.verifyKhaltiPayment(pidx);
        if (shouldRedirectToTicket(request)) {
            URI ticketUri = UriComponentsBuilder
                    .fromUriString(websiteUrl)
                    .path("/bookings")
                    .queryParam("paymentSuccess", true)
                    .queryParam("bookingId", response.getBookingId())
                    .queryParam("paymentId", response.getPaymentId())
                    .queryParam("status", response.getStatus())
                    .queryParam("paymentMethod", response.getPaymentMethod())
                    .queryParam("amount", response.getAmount())
                    .queryParam("transactionId", response.getTransactionId())
                    .queryParam("paidAt", response.getPaidAt())
                    .queryParam("pidx", response.getPidx())
                    .queryParam("message", response.getMessage())
                    .build()
                    .encode()
                    .toUri();

            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(ticketUri)
                    .build();
        }

        return okResponse("Payment verification completed!", response);
    }

    @GetMapping(ApiConstant.PAYMENT_ME)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverPaymentPageResponseDto>> getMyPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paidAt,desc") String sort,
            Authentication authentication) {
        DriverPaymentSearchCriteria criteria = DriverPaymentSearchCriteria.builder()
                .search(search)
                .status(status)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        DriverPaymentPageResponseDto response = driverPaymentService.getPayments(
                authentication.getName(),
                criteria,
                buildPageable(page, size, sort));

        return okResponse("Driver payments fetched successfully!", response);
    }

    @GetMapping(ApiConstant.PAYMENT_ME_BY_ID)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverPaymentHistoryItemResponseDto>> getMyPaymentDetail(
            @PathVariable UUID paymentId,
            Authentication authentication) {
        DriverPaymentHistoryItemResponseDto response = driverPaymentService.getPaymentDetail(
                authentication.getName(),
                paymentId);

        return okResponse("Driver payment detail fetched successfully!", response);
    }

    private boolean shouldRedirectToTicket(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return request.getParameter("status") != null
                || (acceptHeader != null && acceptHeader.contains("text/html"));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be greater than or equal to 0.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100.");
        }

        return PageRequest.of(page, size, parseSort(sort));
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String property = normalizeSortProperty(parts[0]);
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    private String normalizeSortProperty(String property) {
        return switch (property) {
            case "paidAt", "createdAt", "updatedAt", "amount", "status", "paymentMethod" -> property;
            default -> "paidAt";
        };
    }
}
