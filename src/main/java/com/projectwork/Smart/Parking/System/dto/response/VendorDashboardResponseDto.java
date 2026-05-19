package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
//new slots for vehicle added
@Data
public class VendorDashboardResponseDto {
    private int totalParkingLocations;
    private int totalSlots;
    private int availableSlots;
    private int occupiedSlots;
    private VehicleSlotSummary twoWheelerSlots = new VehicleSlotSummary();
    private VehicleSlotSummary fourWheelerSlots = new VehicleSlotSummary();
    private List<LocationSlotSummary> locations = new ArrayList<>();

    @Data
    public static class VehicleSlotSummary {
        private int total;
        private int available;
        private int occupied;
    }

    @Data
    public static class LocationSlotSummary {
        private Long id;
        private String name;
        private int totalSlots;
        private int availableSlots;
        private int occupiedSlots;
        private VehicleSlotSummary twoWheelerSlots = new VehicleSlotSummary();
        private VehicleSlotSummary fourWheelerSlots = new VehicleSlotSummary();
    }
}
