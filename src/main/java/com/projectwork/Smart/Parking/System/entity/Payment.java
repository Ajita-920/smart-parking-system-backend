package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_booking_id", columnList = "booking_id"),
        @Index(name = "idx_payments_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_payment_method", columnList = "payment_method"),
        @Index(name = "idx_payments_pidx", columnList = "pidx")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @NotNull(message = "Booking is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero.")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Payment status is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Size(max = 150, message = "Transaction ID must not exceed 150 characters.")
    @Column(name = "transaction_id", length = 150)
    private String transactionId;

    @NotNull(message = "Payment method is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Size(max = 150, message = "Khalti pidx must not exceed 150 characters.")
    @Column(name = "pidx", length = 150)
    private String pidx;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Size(max = 1000, message = "Payment URL must not exceed 1000 characters.")
    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @PrePersist
    @PreUpdate
    private void beforeSave() {
        if (status == null) {
            status = PaymentStatus.PENDING;
        }

        if (status == PaymentStatus.SUCCESS && paidAt == null) {
            paidAt = Instant.now();
        }
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }
}