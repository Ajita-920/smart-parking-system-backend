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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "parking_slots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_parking_slot_location_slot_number",
                        columnNames = {"location_id", "slot_number"}
                )
        },
        indexes = {
                @Index(name = "idx_parking_slots_location_id", columnList = "location_id"),
                @Index(name = "idx_parking_slots_status", columnList = "status"),
                @Index(name = "idx_parking_slots_vehicle_type", columnList = "vehicle_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlot extends BaseEntity {

    @NotBlank(message = "Slot number is required.")
    @Size(max = 30, message = "Slot number must not exceed 30 characters.")
    @Column(name = "slot_number", nullable = false, length = 30)
    private String slotNumber;

    @NotNull(message = "Vehicle type is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @NotNull(message = "Slot status is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParkingSlotStatus status = ParkingSlotStatus.AVAILABLE;

    @NotNull(message = "Parking location is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private ParkingLocation location;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = ParkingSlotStatus.AVAILABLE;
        }
    }

    public boolean isAvailable() {
        return status == ParkingSlotStatus.AVAILABLE;
    }

    public void markAvailable() {
        this.status = ParkingSlotStatus.AVAILABLE;
    }

    public void markOccupied() {
        this.status = ParkingSlotStatus.OCCUPIED;
    }

    public void markReserved() {
        this.status = ParkingSlotStatus.RESERVED;
    }

    public void markBooked() {
        this.status = ParkingSlotStatus.BOOKED;
    }

    public void markMaintenance() {
        this.status = ParkingSlotStatus.MAINTENANCE;
    }
}
