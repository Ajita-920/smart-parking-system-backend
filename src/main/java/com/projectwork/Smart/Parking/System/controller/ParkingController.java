package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingSlotResponseDto;
import com.projectwork.Smart.Parking.System.service.DijkstraService;
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

/**
 * Public parking search endpoints plus vendor-only parking management actions.
 */
@RestController
@RequestMapping(ApiConstant.PARKING_BASE)
public class ParkingController extends BaseController {

    private final ParkingService parkingService;
    private final DijkstraService dijkstraService;

    public ParkingController(
            ParkingService parkingService,
            DijkstraService dijkstraService) {
        this.parkingService = parkingService;
        this.dijkstraService = dijkstraService;
    }

    /**
     * Lists parking locations with optional area and availability filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getAllParking(
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "true") boolean available) {
        List<ParkingLocationResponseDto> locations = parkingService.getAllParking(area, available);

        if (locations.isEmpty()) {
            return okResponse("No parking locations found.", locations);
        }

        return okResponse("Parking locations fetched successfully!", locations);
    }

    /**
     * Finds nearby parking locations using latitude/longitude distance sorting.
     */
    @GetMapping(ApiConstant.PARKING_NEARBY)
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

    /**
     * Returns the nearest available parking location to the supplied coordinates.
     */
    @GetMapping(ApiConstant.PARKING_NEAREST)
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> getNearestParking(
            @RequestParam double lat,
            @RequestParam double lng) {
        ParkingLocationResponseDto nearest = parkingService.getNearestParking(lat, lng);

        if (nearest == null) {
            return okResponse("No parking spots found near your location.", null);
        }

        return okResponse("Nearest parking spot found!", nearest);
    }

    /**
     * Finds closest locations using direct GPS distance.
     */
    @GetMapping("/nearby-gps")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> findClosestByGps(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") int maxSpots) {
        List<ParkingLocationResponseDto> spots = dijkstraService.findClosestByGps(latitude, longitude, maxSpots);

        if (spots.isEmpty()) {
            return okResponse("No parking spots available.", spots);
        }

        return okResponse("Found " + spots.size() + " parking spots sorted by GPS distance.", spots);
    }

    /**
     * Finds closest Thamel locations using the road-graph/Dijkstra distance model.
     */
    @GetMapping("/thamel/closest")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> findClosestInThamel(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int maxSpots) {
        List<ParkingLocationResponseDto> spots = dijkstraService.findClosestInThamel(latitude, longitude, maxSpots);

        if (spots.isEmpty()) {
            return okResponse("No parking spots available in Thamel.", spots);
        }

        return okResponse(
                "Found " + spots.size() + " parking spots in Thamel sorted by road distance.",
                spots);
    }

    /**
     * Returns the single nearest Thamel parking location.
     */
    @GetMapping("/thamel/nearest")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> findNearestInThamel(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(latitude, longitude);

        if (nearest == null) {
            return okResponse("No parking spots available in Thamel.", null);
        }

        return okResponse("Nearest parking in Thamel found successfully!", nearest);
    }

    /**
     * Lists parking locations owned by the authenticated vendor.
     */
    @GetMapping(ApiConstant.PARKING_MINE)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getMyParkingLocations(
            Authentication authentication) {
        return okResponse(
                "Your parking locations fetched successfully!",
                parkingService.getMyParkingLocations(authentication.getName()));
    }

    /**
     * Fetches a parking location by id.
     */
    @GetMapping(ApiConstant.PARKING_BY_ID)
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> getParkingById(
            @PathVariable UUID id) {
        return okResponse(
                "Parking location fetched successfully!",
                parkingService.getParkingById(id));
    }

    /**
     * Returns public slot data, optionally filtered by vehicle type.
     */
    @GetMapping(ApiConstant.PARKING_SLOTS)
    public ResponseEntity<ApiResponse<List<ParkingSlotResponseDto>>> getParkingSlots(
            @PathVariable UUID id,
            @RequestParam(required = false) String vehicleType) {
        List<ParkingSlotResponseDto> slots = parkingService.getParkingSlots(id, vehicleType);

        if (slots.isEmpty()) {
            return okResponse("No parking slots found.", slots);
        }

        return okResponse("Parking slots fetched successfully!", slots);
    }

    /**
     * Returns all slots for a vendor-owned parking location.
     */
    @GetMapping(ApiConstant.PARKING_ALL_SLOTS)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<ParkingSlotResponseDto>>> getAllSlotsForVendor(
            @PathVariable UUID id,
            Authentication authentication) {
        List<ParkingSlotResponseDto> slots = parkingService.getVendorSlots(id, authentication.getName());

        if (slots.isEmpty()) {
            return okResponse("No slots found for this parking location.", slots);
        }

        return okResponse("Parking slots fetched successfully!", slots);
    }

    /**
     * Creates a parking location for the authenticated vendor.
     */
    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> addParkingLocation(
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {
        return okResponse(
                "Parking location added successfully!",
                parkingService.addParkingLocation(request, authentication.getName()));
    }

    /**
     * Updates a vendor-owned parking location.
     */
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

    /**
     * Updates only the available slot counts for a vendor-owned parking location.
     */
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

    /**
     * Soft-deletes a vendor-owned parking location.
     */
    @DeleteMapping(ApiConstant.PARKING_BY_ID)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteParkingLocation(
            @PathVariable UUID id,
            Authentication authentication) {
        parkingService.deleteParkingLocation(id, authentication.getName());
        return okResponse("Parking location deleted successfully!", null);
    }
}
