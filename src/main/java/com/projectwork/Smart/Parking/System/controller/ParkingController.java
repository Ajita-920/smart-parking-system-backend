package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstant.PARKING_BASE)
public class ParkingController extends BaseController {

    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getAllParking(
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "true") boolean available) {
        List<ParkingLocationResponseDto> locations = parkingService.getAllParking(area, available);

        if (locations.isEmpty()) {
            return okResponse("No parking locations found.", locations);
        }

        return okResponse("Parking locations fetched successfully!", locations);
    }

    @GetMapping(ApiConstant.PARKING_NEARBY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getNearbyParking(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int limit) {
        List<ParkingLocationResponseDto> spots = parkingService.getNearbyParking(lat, lng, limit);

        if (spots.isEmpty()) {
            return okResponse("No parking spots found near your location.", spots);
        }

        return okResponse("Found " + spots.size() + " nearby parking spots.", spots);
    }

    @GetMapping(ApiConstant.PARKING_NEAREST)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> getNearestParking(
            @RequestParam double lat,
            @RequestParam double lng) {
        ParkingLocationResponseDto nearest = parkingService.getNearestParking(lat, lng);

        if (nearest == null) {
            return okResponse("No parking spots found near your location.", null);
        }

        return okResponse("Nearest parking spot found!", nearest);
    }

    @GetMapping(ApiConstant.PARKING_MINE)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getMyParkingLocations(
            Authentication authentication) {
        return okResponse(
                "Your parking locations fetched successfully!",
                parkingService.getMyParkingLocations(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> addParkingLocation(
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {
        return okResponse(
                "Parking location added successfully!",
                parkingService.addParkingLocation(request, authentication.getName()));
    }

    @PutMapping(ApiConstant.PARKING_BY_ID)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> updateParkingLocation(
            @PathVariable UUID id,
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {
        return okResponse(
                "Parking location updated successfully!",
                parkingService.updateParkingLocation(id, request, authentication.getName()));
    }

    @PatchMapping(ApiConstant.PARKING_SLOTS)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> updateAvailableSlots(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSlotsRequestDto request,
            Authentication authentication) {
        return okResponse(
                "Available slots updated successfully!",
                parkingService.updateAvailableSlots(id, request, authentication.getName()));
    }

    @DeleteMapping(ApiConstant.PARKING_BY_ID)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteParkingLocation(
            @PathVariable UUID id,
            Authentication authentication) {
        parkingService.deleteParkingLocation(id, authentication.getName());
        return okResponse("Parking location deleted successfully!", null);
    }
}
