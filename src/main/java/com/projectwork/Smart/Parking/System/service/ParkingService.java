package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingSlotResponseDto;

import java.util.List;
import java.util.UUID;

public interface ParkingService {

    List<ParkingLocationResponseDto> getAllParking(String area, boolean available);

    List<ParkingLocationResponseDto> getNearbyParking(double latitude, double longitude, int limit);

    ParkingLocationResponseDto getNearestParking(double latitude, double longitude);

    ParkingLocationResponseDto getParkingById(UUID id);

    List<ParkingLocationResponseDto> getMyParkingLocations(String currentUserEmail);

    ParkingLocationResponseDto addParkingLocation(ParkingLocationRequestDto request, String currentUserEmail);

    ParkingLocationResponseDto updateParkingLocation(UUID id, ParkingLocationRequestDto request,
            String currentUserEmail);

    ParkingLocationResponseDto updateAvailableSlots(UUID id, UpdateSlotsRequestDto request, String currentUserEmail);

    void deleteParkingLocation(UUID id, String currentUserEmail);

    List<ParkingSlotResponseDto> getParkingSlots(UUID parkingLocationId, String vehicleType);

    List<ParkingSlotResponseDto> getVendorSlots(UUID parkingLocationId, String currentUserEmail);
}
