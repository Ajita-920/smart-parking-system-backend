package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_locations", indexes = {
        @Index(name = "idx_parking_locations_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_parking_locations_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLocation extends BaseEntity {

    private static final double DEFAULT_TWO_WHEELER_RATE = 50.0;
    private static final double DEFAULT_FOUR_WHEELER_RATE = 100.0;

    @NotBlank(message = "Parking location name is required.")
    @Size(min = 2, max = 100, message = "Parking location name must be between 2 and 100 characters.")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Address is required.")
    @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters.")
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @NotNull(message = "Total four-wheeler slots is required.")
    @Min(value = 0, message = "Total four-wheeler slots cannot be negative.")
    @Column(name = "total_four_wheeler_slots", nullable = false)
    private Integer totalFourWheelerSlots;

    @NotNull(message = "Available four-wheeler slots is required.")
    @Min(value = 0, message = "Available four-wheeler slots cannot be negative.")
    @Column(name = "available_four_wheeler_slots", nullable = false)
    private Integer availableFourWheelerSlots;

    @NotNull(message = "Total two-wheeler slots is required.")
    @Min(value = 0, message = "Total two-wheeler slots cannot be negative.")
    @Column(name = "total_two_wheeler_slots", nullable = false)
    private Integer totalTwoWheelerSlots;

    @NotNull(message = "Available two-wheeler slots is required.")
    @Min(value = 0, message = "Available two-wheeler slots cannot be negative.")
    @Column(name = "available_two_wheeler_slots", nullable = false)
    private Integer availableTwoWheelerSlots;

    @DecimalMin(value = "0.0", inclusive = false, message = "Two-wheeler rate must be greater than zero.")
    @Column(name = "two_wheeler_rate_per_hour")
    private Double twoWheelerRatePerHour = DEFAULT_TWO_WHEELER_RATE;

    @DecimalMin(value = "0.0", inclusive = false, message = "Four-wheeler rate must be greater than zero.")
    @Column(name = "four_wheeler_rate_per_hour")
    private Double fourWheelerRatePerHour = DEFAULT_FOUR_WHEELER_RATE;

    @NotNull(message = "Vendor is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    public Integer getTotalSlots() {
        int fourWheeler = totalFourWheelerSlots != null ? totalFourWheelerSlots : 0;
        int twoWheeler = totalTwoWheelerSlots != null ? totalTwoWheelerSlots : 0;
        return fourWheeler + twoWheeler;
    }

    public Integer getAvailableSlots() {
        int fourWheeler = availableFourWheelerSlots != null ? availableFourWheelerSlots : 0;
        int twoWheeler = availableTwoWheelerSlots != null ? availableTwoWheelerSlots : 0;
        return fourWheeler + twoWheeler;
    }

    @PrePersist
    @PreUpdate
    private void validateSlots() {
        if (availableFourWheelerSlots == null && totalFourWheelerSlots != null) {
            availableFourWheelerSlots = totalFourWheelerSlots;
        }

        if (availableTwoWheelerSlots == null && totalTwoWheelerSlots != null) {
            availableTwoWheelerSlots = totalTwoWheelerSlots;
        }

        if (twoWheelerRatePerHour == null) {
            twoWheelerRatePerHour = DEFAULT_TWO_WHEELER_RATE;
        }

        if (fourWheelerRatePerHour == null) {
            fourWheelerRatePerHour = DEFAULT_FOUR_WHEELER_RATE;
        }

        if (availableFourWheelerSlots != null
                && totalFourWheelerSlots != null
                && availableFourWheelerSlots > totalFourWheelerSlots) {
            throw new IllegalArgumentException(
                    "Available four-wheeler slots cannot be greater than total four-wheeler slots.");
        }

        if (availableTwoWheelerSlots != null
                && totalTwoWheelerSlots != null
                && availableTwoWheelerSlots > totalTwoWheelerSlots) {
            throw new IllegalArgumentException(
                    "Available two-wheeler slots cannot be greater than total two-wheeler slots.");
        }
    }
}