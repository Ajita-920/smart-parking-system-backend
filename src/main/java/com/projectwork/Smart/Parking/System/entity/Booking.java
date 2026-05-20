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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_driver_id", columnList = "driver_id"),
        @Index(name = "idx_bookings_slot_id", columnList = "slot_id"),
        @Index(name = "idx_bookings_location_id", columnList = "location_id"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_start_time", columnList = "start_time"),
        @Index(name = "idx_bookings_end_time", columnList = "end_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @NotNull(message = "Driver is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @NotNull(message = "Parking slot is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ParkingSlot slot;

    @NotNull(message = "Parking location is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private ParkingLocation parkingLocation;

    @NotNull(message = "Start time is required.")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "Booking status is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount cannot be negative.")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    private void validateBooking() {
        if (status == null) {
            status = BookingStatus.PENDING;
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        if (parkingLocation == null && slot != null) {
            parkingLocation = slot.getLocation();
        }
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    public void markPending() {
        this.status = BookingStatus.PENDING;
    }

    public void markConfirmed() {
        this.status = BookingStatus.CONFIRMED;
    }

    public void markCancelled() {
        this.status = BookingStatus.CANCELLED;
    }

    public void markCompleted() {
        this.status = BookingStatus.COMPLETED;
    }
}