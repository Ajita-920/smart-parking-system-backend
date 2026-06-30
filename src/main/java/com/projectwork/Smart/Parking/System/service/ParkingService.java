package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingSlotResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Parking search, detail, slot, and vendor-management use cases.
 */
public interface ParkingService {

    /**
     * Lists parking locations using optional area and availability filters.
     */
    List<ParkingLocationResponseDto> getAllParking(String area, boolean available);

    /**
     * Finds nearby parking locations by direct coordinate distance.
     */
    List<ParkingLocationResponseDto> getNearbyParkingByRoadDistance(double latitude, double longitude, int limit);

    /**
     * Returns the nearest parking location by direct coordinate distance.
     */
    ParkingLocationResponseDto getSingleNearestParking(double latitude, double longitude);

    /**
     * Fetches a parking location by id.
     */
    ParkingLocationResponseDto getParkingById(UUID id);

    /**
     * Lists locations owned by the current vendor.
     */
    List<ParkingLocationResponseDto> getMyParkingLocations(String currentUserEmail);

    /**
     * Creates a parking location for the current vendor.
     */
    ParkingLocationResponseDto addParkingLocation(ParkingLocationRequestDto request, String currentUserEmail);

    /**
     * Updates a current vendor's parking location.
     */
    ParkingLocationResponseDto updateParkingLocation(UUID id, ParkingLocationRequestDto request,
            String currentUserEmail);

    /**
     * Updates only available slot counts for a current vendor's parking location.
     */
    ParkingLocationResponseDto updateAvailableSlots(UUID id, UpdateSlotsRequestDto request, String currentUserEmail);

    /**
     * Soft-deletes a current vendor's parking location.
     */
    void deleteParkingLocation(UUID id, String currentUserEmail);

    /**
     * Returns public slot information, optionally filtered by vehicle type.
     */
    List<ParkingSlotResponseDto> getParkingSlots(UUID parkingLocationId, String vehicleType);

    /**
     * Returns all slot information for a vendor-owned location.
     */
    List<ParkingSlotResponseDto> getVendorSlots(UUID parkingLocationId, String currentUserEmail);
}
