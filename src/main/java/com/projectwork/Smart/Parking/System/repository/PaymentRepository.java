package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Payment> findByPidxAndDeletedAtIsNull(String pidx);

    Optional<Payment> findByTransactionIdAndDeletedAtIsNull(String transactionId);

    Optional<Payment> findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
            UUID bookingId,
            PaymentStatus status);

    List<Payment> findByBookingAndDeletedAtIsNull(Booking booking);

    List<Payment> findByBooking_IdAndDeletedAtIsNull(UUID bookingId);

    List<Payment> findByStatusAndDeletedAtIsNull(PaymentStatus status);

    List<Payment> findByPaymentMethodAndDeletedAtIsNull(PaymentMethod paymentMethod);

    long countByStatusAndDeletedAtIsNull(PaymentStatus status);

    long countByPaymentMethodAndDeletedAtIsNull(PaymentMethod paymentMethod);
}