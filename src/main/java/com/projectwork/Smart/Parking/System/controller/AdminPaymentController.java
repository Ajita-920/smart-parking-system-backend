package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.AdminPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentDetailResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentPageResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentSummaryResponseDto;
import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.service.AdminPaymentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstant.ADMIN_BASE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController extends BaseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping(ApiConstant.ADMIN_PAYMENT_SUMMARY)
    public ResponseEntity<ApiResponse<AdminPaymentSummaryResponseDto>> getPaymentSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        AdminPaymentSearchCriteria criteria = AdminPaymentSearchCriteria.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        return okResponse(
                "Admin payment summary fetched successfully!",
                adminPaymentService.getPaymentSummary(criteria));
    }

    @GetMapping(ApiConstant.ADMIN_PAYMENTS)
    public ResponseEntity<ApiResponse<AdminPaymentPageResponseDto>> getPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paidAt,desc") String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        AdminPaymentSearchCriteria criteria = AdminPaymentSearchCriteria.builder()
                .search(search)
                .status(status)
                .method(method)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        return okResponse(
                "Admin payments fetched successfully!",
                adminPaymentService.getPayments(criteria, pageable));
    }

    @GetMapping(ApiConstant.ADMIN_PAYMENT_BY_ID)
    public ResponseEntity<ApiResponse<AdminPaymentDetailResponseDto>> getPaymentDetail(
            @PathVariable UUID paymentId
    ) {
        return okResponse(
                "Admin payment detail fetched successfully!",
                adminPaymentService.getPaymentDetail(paymentId));
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
