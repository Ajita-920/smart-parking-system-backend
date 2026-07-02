package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class VendorDashboardResponseDto {

    private int totalParkingLocations;
    private int totalSlots;
    private int availableSlots;
    private int occupiedSlots;

    private VehicleSlotSummary twoWheelerSlots = new VehicleSlotSummary();
    private VehicleSlotSummary fourWheelerSlots = new VehicleSlotSummary();

    private List<LocationSlotSummary> locations = new ArrayList<>();
    private VendorSummary vendor;

    @Data
    public static class VehicleSlotSummary {
        private int total;
        private int available;
        private int occupied;
    }

    @Data
    public static class LocationSlotSummary {
        private UUID id;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double twoWheelerRatePerHour;
        private Double fourWheelerRatePerHour;
        private String vendorName;

        private int totalSlots;
        private int availableSlots;
        private int occupiedSlots;

        private VehicleSlotSummary twoWheelerSlots = new VehicleSlotSummary();
        private VehicleSlotSummary fourWheelerSlots = new VehicleSlotSummary();
    }

    @Data
    public static class VendorSummary {
        private UUID id;
        private String name;
        private String email;
        private boolean approved;
    }
}
