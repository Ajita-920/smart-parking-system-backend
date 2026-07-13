package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.AdminPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentBookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentCustomerResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentDetailResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentListItemResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentPageResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentParkingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentRefundResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentSummaryResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.RefundStatus;
import com.projectwork.Smart.Parking.System.entity.User;
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
import java.util.UUID;

@Service
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;

    public AdminPaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentSummaryResponseDto getPaymentSummary(AdminPaymentSearchCriteria criteria) {
        validateDateRange(criteria);

        List<Payment> payments = paymentRepository.findAll(buildSpecification(criteria));
        AdminPaymentSummaryResponseDto dto = new AdminPaymentSummaryResponseDto();

        BigDecimal totalRevenue = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setTotalRevenue(totalRevenue);
        dto.setSuccessfulCount(payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS).count());
        dto.setPendingCount(payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING).count());
        dto.setFailedCount(payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentPageResponseDto getPayments(AdminPaymentSearchCriteria criteria, Pageable pageable) {
        validateDateRange(criteria);

        Page<Payment> paymentPage = paymentRepository.findAll(buildSpecification(criteria), pageable);
        AdminPaymentPageResponseDto dto = new AdminPaymentPageResponseDto();

        dto.setContent(paymentPage.getContent().stream().map(this::toListItem).toList());
        dto.setPage(paymentPage.getNumber());
        dto.setSize(paymentPage.getSize());
        dto.setTotalElements(paymentPage.getTotalElements());
        dto.setTotalPages(paymentPage.getTotalPages());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentDetailResponseDto getPaymentDetail(UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found."));

        return toDetail(payment);
    }

    private Specification<Payment> buildSpecification(AdminPaymentSearchCriteria criteria) {
        return (root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var bookingFetch = root.fetch("booking", JoinType.LEFT);
                bookingFetch.fetch("driver", JoinType.LEFT);
                bookingFetch.fetch("parkingLocation", JoinType.LEFT).fetch("vendor", JoinType.LEFT);
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

            if (criteria.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getMethod() != null) {
                predicates.add(builder.equal(root.get("paymentMethod"), criteria.getMethod()));
            }

            addDatePredicates(criteria, root, builder, predicates);
            addSearchPredicates(criteria, root, builder, booking, driver, parkingLocation, slot, predicates);

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addDatePredicates(
            AdminPaymentSearchCriteria criteria,
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

    private void addSearchPredicates(
            AdminPaymentSearchCriteria criteria,
            jakarta.persistence.criteria.Root<Payment> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            Join<Payment, Booking> booking,
            Join<Booking, User> driver,
            Join<Booking, ParkingLocation> parkingLocation,
            Join<Booking, ParkingSlot> slot,
            List<Predicate> predicates
    ) {
        if (criteria.getSearch() == null || criteria.getSearch().isBlank()) {
            return;
        }

        String search = "%" + criteria.getSearch().trim().toLowerCase() + "%";

        predicates.add(builder.or(
                builder.like(builder.lower(root.get("id").as(String.class)), search),
                builder.like(builder.lower(booking.get("id").as(String.class)), search),
                builder.like(builder.lower(root.get("transactionId")), search),
                builder.like(builder.lower(booking.get("customerName")), search),
                builder.like(builder.lower(booking.get("customerPhone")), search),
                builder.like(builder.lower(booking.get("vehicleNumber")), search),
                builder.like(builder.lower(driver.get("name")), search),
                builder.like(builder.lower(driver.get("phone")), search),
                builder.like(builder.lower(parkingLocation.get("name")), search),
                builder.like(builder.lower(slot.get("slotNumber")), search)
        ));
    }

    private AdminPaymentListItemResponseDto toListItem(Payment payment) {
        Booking booking = payment.getBooking();
        AdminPaymentListItemResponseDto dto = new AdminPaymentListItemResponseDto();

        dto.setPaymentId(payment.getId());
        dto.setBookingId(booking != null ? booking.getId() : null);
        dto.setCustomerName(resolveCustomerName(booking));
        dto.setCustomerPhone(resolveCustomerPhone(booking));
        dto.setCustomerRole(booking != null && booking.getDriver() != null ? booking.getDriver().getRole() : null);
        dto.setParkingLocationName(resolveParkingLocationName(booking));
        dto.setSlotCode(resolveSlotCode(booking));
        dto.setVehicleNumber(booking != null ? booking.getVehicleNumber() : null);
        dto.setVehicleType(booking != null ? booking.getVehicleType() : null);
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setKhaltiPidx(payment.getPidx());
        dto.setPaidAt(payment.getPaidAt());
        dto.setBookingStartTime(booking != null ? booking.getStartTime() : null);
        dto.setBookingEndTime(booking != null ? booking.getEndTime() : null);
        dto.setBookingSource(booking != null && booking.isWalkIn() ? "WALK_IN" : "ONLINE");

        return dto;
    }

    private AdminPaymentDetailResponseDto toDetail(Payment payment) {
        AdminPaymentDetailResponseDto dto = new AdminPaymentDetailResponseDto();
        Booking booking = payment.getBooking();

        dto.setPaymentId(payment.getId());
        dto.setBookingId(booking != null ? booking.getId() : null);
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setKhaltiPidx(payment.getPidx());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        dto.setCustomer(toCustomer(booking));
        dto.setBooking(toBooking(booking));
        dto.setParking(toParking(booking));
        dto.setRefund(toRefund(payment));

        return dto;
    }

    private AdminPaymentCustomerResponseDto toCustomer(Booking booking) {
        AdminPaymentCustomerResponseDto dto = new AdminPaymentCustomerResponseDto();

        if (booking == null) {
            return dto;
        }

        User driver = booking.getDriver();
        dto.setId(driver != null ? driver.getId() : null);
        dto.setName(resolveCustomerName(booking));
        dto.setEmail(driver != null ? driver.getEmail() : null);
        dto.setPhone(resolveCustomerPhone(booking));
        dto.setRole(driver != null ? driver.getRole() : null);
        dto.setVehicleNumber(booking.getVehicleNumber());

        return dto;
    }

    private AdminPaymentBookingResponseDto toBooking(Booking booking) {
        AdminPaymentBookingResponseDto dto = new AdminPaymentBookingResponseDto();

        if (booking == null) {
            return dto;
        }

        dto.setId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setSource(booking.isWalkIn() ? "WALK_IN" : "ONLINE");
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setDurationMinutes(resolveDurationMinutes(booking));
        dto.setVehicleType(booking.getVehicleType());
        dto.setVehicleNumber(booking.getVehicleNumber());

        return dto;
    }

    private AdminPaymentParkingResponseDto toParking(Booking booking) {
        AdminPaymentParkingResponseDto dto = new AdminPaymentParkingResponseDto();

        if (booking == null) {
            return dto;
        }

        ParkingLocation location = booking.getParkingLocation();
        ParkingSlot slot = booking.getSlot();
        User vendor = location != null ? location.getVendor() : null;

        dto.setId(location != null ? location.getId() : null);
        dto.setName(location != null ? location.getName() : null);
        dto.setSlotId(slot != null ? slot.getId() : null);
        dto.setSlotCode(slot != null ? slot.getSlotNumber() : null);
        dto.setVendorId(vendor != null ? vendor.getId() : null);
        dto.setVendorName(vendor != null ? vendor.getName() : null);

        return dto;
    }

    private AdminPaymentRefundResponseDto toRefund(Payment payment) {
        if (payment.getRefundStatus() == null || payment.getRefundStatus() == RefundStatus.NONE) {
            return null;
        }

        AdminPaymentRefundResponseDto dto = new AdminPaymentRefundResponseDto();
        dto.setEligible(payment.getRefundStatus() == RefundStatus.PENDING);
        dto.setAmount(payment.getRefundAmount());
        dto.setStatus(payment.getRefundStatus());
        dto.setReason(payment.getRefundStatus() == RefundStatus.PENDING
                ? "Refund has been requested for this payment."
                : "Refund status is " + payment.getRefundStatus().name().toLowerCase() + ".");

        return dto;
    }

    private String resolveCustomerName(Booking booking) {
        if (booking == null) {
            return "Unknown customer";
        }

        if (booking.getCustomerName() != null && !booking.getCustomerName().isBlank()) {
            return booking.getCustomerName();
        }

        if (booking.getDriver() != null) {
            return booking.getDriver().getName();
        }

        return booking.isWalkIn() ? "Walk-in customer" : "Unknown customer";
    }

    private String resolveCustomerPhone(Booking booking) {
        if (booking == null) {
            return null;
        }

        if (booking.getCustomerPhone() != null && !booking.getCustomerPhone().isBlank()) {
            return booking.getCustomerPhone();
        }

        return booking.getDriver() != null ? booking.getDriver().getPhone() : null;
    }

    private String resolveParkingLocationName(Booking booking) {
        return booking != null && booking.getParkingLocation() != null
                ? booking.getParkingLocation().getName()
                : null;
    }

    private String resolveSlotCode(Booking booking) {
        return booking != null && booking.getSlot() != null
                ? booking.getSlot().getSlotNumber()
                : null;
    }

    private Long resolveDurationMinutes(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return null;
        }

        return Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
    }

    private void validateDateRange(AdminPaymentSearchCriteria criteria) {
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
}
