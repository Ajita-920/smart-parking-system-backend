package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.DriverPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentBookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentHistoryItemResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentPageResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentRefundResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentSummaryResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.RefundStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DriverPaymentServiceImpl implements DriverPaymentService {

    private final PaymentRepository paymentRepository;

    public DriverPaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverPaymentPageResponseDto getPayments(
            String currentUserEmail,
            DriverPaymentSearchCriteria criteria,
            Pageable pageable
    ) {
        validateDateRange(criteria);

        Specification<Payment> specification = buildSpecification(currentUserEmail, criteria);
        Page<Payment> paymentPage = paymentRepository.findAll(specification, pageable);
        List<Payment> allMatchingPayments = paymentRepository.findAll(specification);

        DriverPaymentPageResponseDto dto = new DriverPaymentPageResponseDto();
        dto.setContent(paymentPage.getContent().stream().map(this::toHistoryItem).toList());
        dto.setPage(paymentPage.getNumber());
        dto.setSize(paymentPage.getSize());
        dto.setTotalElements(paymentPage.getTotalElements());
        dto.setTotalPages(paymentPage.getTotalPages());
        dto.setSummary(toSummary(allMatchingPayments));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverPaymentHistoryItemResponseDto getPaymentDetail(String currentUserEmail, UUID paymentId) {
        Payment payment = paymentRepository.findOne((root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var bookingFetch = root.fetch("booking", JoinType.LEFT);
                bookingFetch.fetch("driver", JoinType.LEFT);
                bookingFetch.fetch("parkingLocation", JoinType.LEFT);
                bookingFetch.fetch("slot", JoinType.LEFT);
            }
            query.distinct(true);

            Join<Payment, Booking> booking = root.join("booking", JoinType.LEFT);
            Join<Booking, User> driver = booking.join("driver", JoinType.LEFT);

            return builder.and(
                    builder.equal(root.get("id"), paymentId),
                    builder.isNull(root.get("deletedAt")),
                    builder.isNull(booking.get("deletedAt")),
                    builder.equal(builder.lower(driver.get("email")), normalizeEmail(currentUserEmail))
            );
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));

        return toHistoryItem(payment);
    }

    private Specification<Payment> buildSpecification(String currentUserEmail, DriverPaymentSearchCriteria criteria) {
        return (root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var bookingFetch = root.fetch("booking", JoinType.LEFT);
                bookingFetch.fetch("driver", JoinType.LEFT);
                bookingFetch.fetch("parkingLocation", JoinType.LEFT);
                bookingFetch.fetch("slot", JoinType.LEFT);
            }
            query.distinct(true);

            Join<Payment, Booking> booking = root.join("booking", JoinType.LEFT);
            Join<Booking, User> driver = booking.join("driver", JoinType.LEFT);
            Join<Booking, ParkingLocation> parkingLocation = booking.join("parkingLocation", JoinType.LEFT);
            Join<Booking, ParkingSlot> slot = booking.join("slot", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            predicates.add(builder.isNull(booking.get("deletedAt")));
            predicates.add(builder.equal(builder.lower(driver.get("email")), normalizeEmail(currentUserEmail)));

            if (criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }

            addDatePredicates(criteria, root, builder, predicates);
            addSearchPredicates(criteria, root, builder, booking, parkingLocation, slot, predicates);

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addSearchPredicates(
            DriverPaymentSearchCriteria criteria,
            jakarta.persistence.criteria.Root<Payment> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            Join<Payment, Booking> booking,
            Join<Booking, ParkingLocation> parkingLocation,
            Join<Booking, ParkingSlot> slot,
            List<Predicate> predicates
    ) {
        if (criteria.getSearch() == null || criteria.getSearch().isBlank()) {
            return;
        }

        String search = "%" + criteria.getSearch().trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(builder.or(
                builder.like(builder.lower(root.get("id").as(String.class)), search),
                builder.like(builder.lower(booking.get("id").as(String.class)), search),
                builder.like(builder.lower(root.get("transactionId")), search),
                builder.like(builder.lower(root.get("pidx")), search),
                builder.like(builder.lower(booking.get("vehicleNumber")), search),
                builder.like(builder.lower(parkingLocation.get("name")), search),
                builder.like(builder.lower(parkingLocation.get("address")), search),
                builder.like(builder.lower(slot.get("slotNumber")), search)
        ));
    }

    private void addDatePredicates(
            DriverPaymentSearchCriteria criteria,
            jakarta.persistence.criteria.Root<Payment> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            List<Predicate> predicates
    ) {
        Instant from = startOfDay(criteria.getFromDate());
        Instant to = endOfDay(criteria.getToDate());

        if (from == null && to == null) {
            return;
        }

        jakarta.persistence.criteria.Expression<Instant> effectiveDate =
                builder.coalesce(root.get("paidAt"), root.get("createdAt"));

        if (from != null) {
            predicates.add(builder.greaterThanOrEqualTo(effectiveDate, from));
        }

        if (to != null) {
            predicates.add(builder.lessThanOrEqualTo(effectiveDate, to));
        }
    }

    private DriverPaymentHistoryItemResponseDto toHistoryItem(Payment payment) {
        Booking booking = payment.getBooking();
        DriverPaymentHistoryItemResponseDto dto = new DriverPaymentHistoryItemResponseDto();

        dto.setPaymentId(payment.getId());
        dto.setBookingId(booking != null ? booking.getId() : null);
        dto.setTransactionId(payment.getTransactionId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentUrl(payment.getPaymentUrl());
        dto.setPidx(payment.getPidx());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        dto.setMessage(resolveMessage(payment));
        dto.setBooking(toBooking(booking));
        dto.setRefund(toRefund(payment));

        return dto;
    }

    private DriverPaymentBookingResponseDto toBooking(Booking booking) {
        DriverPaymentBookingResponseDto dto = new DriverPaymentBookingResponseDto();
        if (booking == null) {
            return dto;
        }

        ParkingLocation location = booking.getParkingLocation();
        ParkingSlot slot = booking.getSlot();

        dto.setBookingId(booking.getId());
        dto.setParkingLocationId(location != null ? location.getId() : null);
        dto.setParkingLocationName(location != null ? location.getName() : null);
        dto.setAddress(location != null ? location.getAddress() : null);
        dto.setSlotId(slot != null ? slot.getId() : null);
        dto.setSlotNumber(slot != null ? slot.getSlotNumber() : null);
        dto.setVehicleNumber(booking.getVehicleNumber());
        dto.setVehicleType(booking.getVehicleType());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setDurationMinutes(resolveDurationMinutes(booking));
        dto.setRatePerHour(resolveRatePerHour(booking));
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setStatus(booking.getStatus());

        return dto;
    }

    private DriverPaymentRefundResponseDto toRefund(Payment payment) {
        DriverPaymentRefundResponseDto dto = new DriverPaymentRefundResponseDto();
        RefundStatus status = payment.getRefundStatus() != null ? payment.getRefundStatus() : RefundStatus.NONE;

        dto.setRefundStatus(status);
        dto.setRefundEligible(status == RefundStatus.PENDING);
        dto.setRefundAmount(payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO);

        return dto;
    }

    private DriverPaymentSummaryResponseDto toSummary(List<Payment> payments) {
        DriverPaymentSummaryResponseDto summary = new DriverPaymentSummaryResponseDto();
        summary.setTotalPaymentCount(payments.size());

        for (Payment payment : payments) {
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                summary.setSuccessfulPaymentCount(summary.getSuccessfulPaymentCount() + 1);
                summary.setSuccessfulAmount(summary.getSuccessfulAmount().add(amount));
                summary.setTotalSpent(summary.getTotalSpent().add(amount));
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                summary.setPendingPaymentCount(summary.getPendingPaymentCount() + 1);
                summary.setPendingAmount(summary.getPendingAmount().add(amount));
            } else if (payment.getStatus() == PaymentStatus.FAILED) {
                summary.setFailedPaymentCount(summary.getFailedPaymentCount() + 1);
                summary.setFailedAmount(summary.getFailedAmount().add(amount));
            }
        }

        return summary;
    }

    private Long resolveDurationMinutes(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return null;
        }

        return Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
    }

    private Double resolveRatePerHour(Booking booking) {
        ParkingLocation location = booking.getParkingLocation();
        if (location == null || booking.getVehicleType() == null) {
            return null;
        }

        return booking.getVehicleType() == VehicleType.TWO_WHEELER
                ? location.getTwoWheelerRatePerHour()
                : location.getFourWheelerRatePerHour();
    }

    private String resolveMessage(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return "Payment completed successfully.";
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            return "Payment is awaiting Khalti verification.";
        }

        return "Payment failed. You can retry the Khalti payment for this booking.";
    }

    private void validateDateRange(DriverPaymentSearchCriteria criteria) {
        if (criteria.getFromDate() != null
                && criteria.getToDate() != null
                && criteria.getFromDate().isAfter(criteria.getToDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "fromDate must be before or equal to toDate.");
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
    }

    private Instant endOfDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1) : null;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
