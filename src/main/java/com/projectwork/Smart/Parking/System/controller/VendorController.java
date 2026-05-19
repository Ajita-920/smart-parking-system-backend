package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.VendorPricingUpdateRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.VendorSlotManagementRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({ApiConstant.VENDOR_BASE})
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('VENDOR')")   // Only VENDOR role can access these endpoints
public class VendorController extends BaseController {

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    @Autowired
    private UserRepository userRepository;

    // ADD NEW PARKING LOCATION ====================
    @PostMapping({ApiConstant.VENDOR_ADD_PARKING_LEGACY})
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> addParkingLocation(
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {

        String email = authentication.getName();
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));

        if (request.getTotalSlots() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total slots must be positive");
        }
//wheeler rate per hour added new
        ParkingLocation parking = new ParkingLocation();
        parking.setName(request.getName());
        parking.setAddress(request.getAddress());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());
        parking.setTotalSlots(request.getTotalSlots());
        parking.setAvailableSlots(request.getTotalSlots());
        parking.setTwoWheelerRatePerHour(request.getTwoWheelerRatePerHour() != null ? request.getTwoWheelerRatePerHour() : 50.0);
        parking.setFourWheelerRatePerHour(request.getFourWheelerRatePerHour() != null ? request.getFourWheelerRatePerHour() : 100.0);
        parking.setVendor(vendor);

        ParkingLocation saved = parkingLocationRepository.save(parking);

        return okResponse("Parking location added successfully!", mapToResponse(saved));
    }

    // GET MY PARKING LOCATIONS
    @GetMapping({ApiConstant.VENDOR_PARKING_LOCATIONS})
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getMyParkingLocations(Authentication authentication) {
        String email = authentication.getName();
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        List<ParkingLocation> parkings = parkingLocationRepository.findByVendor(vendor);

        List<ParkingLocationResponseDto> responseList = parkings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return okResponse("My parking locations fetched!", responseList);
    }

    // ==================== UPDATE AVAILABLE SLOTS ====================
    //new vendor updating parking slots
    @PutMapping({ApiConstant.VENDOR_UPDATE_PARKING_LEGACY})
    public ResponseEntity<?> updateAvailableSlots(
            @PathVariable Long id,
            @RequestParam(required = false) Integer availableSlots,
            @RequestParam(required = false, name = "newAvailableSlots") Integer legacyAvailableSlots,
            Authentication authentication) {

        ParkingLocation parking = parkingLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking location not found"));

        // Security check: Only owner vendor can update
        String email = authentication.getName();
        if (!parking.getVendor().getEmail().equals(email)) {
            throw new RuntimeException("You can only update your own parking!");
        }

        Integer nextAvailableSlots = availableSlots != null ? availableSlots : legacyAvailableSlots;
        if (nextAvailableSlots == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "availableSlots is required");
        }
        if (nextAvailableSlots < 0 || nextAvailableSlots > parking.getTotalSlots()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "availableSlots must be between 0 and total slots");
        }

        parking.setAvailableSlots(nextAvailableSlots);
        parkingLocationRepository.save(parking);

        return okResponse("Available slots updated successfully!", new HashMap<>());
    }
//new vendor managing slot
    @PutMapping(ApiConstant.VENDOR_MANAGE_SLOT)
    public ResponseEntity<ApiResponse<Map<String, Object>>> manageSlots(
            @PathVariable Long id,
            @Valid @RequestBody VendorSlotManagementRequestDto request,
            Authentication authentication) {
        ParkingLocation parking = getVendorParkingLocationOrThrow(id, authentication.getName());

        int slotCount = request.getSlotCount();
        if (slotCount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slotCount must be positive");
        }

        int currentAvailable = parking.getAvailableSlots();
        int updatedAvailable = request.getAllocate()
                ? currentAvailable - slotCount
                : currentAvailable + slotCount;

        if (updatedAvailable < 0 || updatedAvailable > parking.getTotalSlots()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot operation exceeds allowed range (0 to total slots)");
        }

        parking.setAvailableSlots(updatedAvailable);
        parkingLocationRepository.save(parking);

        Map<String, Object> response = new HashMap<>();
        response.put("parkingLocationId", parking.getId());
        response.put("availableSlots", parking.getAvailableSlots());
        response.put("totalSlots", parking.getTotalSlots());
        response.put("operation", request.getAllocate() ? "ALLOCATE" : "FREE");
        response.put("slotCount", slotCount);

        return okResponse("Slot operation completed successfully!", response);
    }
//new one
    @PutMapping(ApiConstant.VENDOR_UPDATE_PARKING_RATE)
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> updateParkingRates(
            @PathVariable Long id,
            @Valid @RequestBody VendorPricingUpdateRequestDto request,
            Authentication authentication) {
        ParkingLocation parking = getVendorParkingLocationOrThrow(id, authentication.getName());

        if (request.getTwoWheelerRatePerHour() != null) {
            parking.setTwoWheelerRatePerHour(request.getTwoWheelerRatePerHour());
        }
        if (request.getFourWheelerRatePerHour() != null) {
            parking.setFourWheelerRatePerHour(request.getFourWheelerRatePerHour());
        }

        parkingLocationRepository.save(parking);
        return okResponse("Parking rates updated successfully!", mapToResponse(parking));
    }

    @GetMapping({ApiConstant.VENDOR_DASHBOARD, ApiConstant.VENDOR_DASHBOARD_SUMMARY})
    public ResponseEntity<ApiResponse<VendorDashboardResponseDto>> getVendorDashboard(Authentication authentication) {
        String email = authentication.getName();
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        List<ParkingLocation> parkings = parkingLocationRepository.findByVendor(vendor);
        VendorDashboardResponseDto dashboard = new VendorDashboardResponseDto();
        dashboard.setTotalParkingLocations(parkings.size());

        List<VendorDashboardResponseDto.LocationSlotSummary> locationSummaries = new ArrayList<>();
        for (ParkingLocation parking : parkings) {
            SlotBreakdown split = splitSlots(parking.getTotalSlots(), parking.getAvailableSlots());

            VendorDashboardResponseDto.LocationSlotSummary locationSummary =
                    new VendorDashboardResponseDto.LocationSlotSummary();
            locationSummary.setId(parking.getId());
            locationSummary.setName(parking.getName());
            locationSummary.setTotalSlots(parking.getTotalSlots());
            locationSummary.setAvailableSlots(split.availableSlots);
            locationSummary.setOccupiedSlots(split.occupiedSlots);

            locationSummary.getTwoWheelerSlots().setTotal(split.twoWheelerTotalSlots);
            locationSummary.getTwoWheelerSlots().setAvailable(split.twoWheelerAvailableSlots);
            locationSummary.getTwoWheelerSlots().setOccupied(split.twoWheelerOccupiedSlots);

            locationSummary.getFourWheelerSlots().setTotal(split.fourWheelerTotalSlots);
            locationSummary.getFourWheelerSlots().setAvailable(split.fourWheelerAvailableSlots);
            locationSummary.getFourWheelerSlots().setOccupied(split.fourWheelerOccupiedSlots);

            locationSummaries.add(locationSummary);

            dashboard.setTotalSlots(dashboard.getTotalSlots() + split.totalSlots);
            dashboard.setAvailableSlots(dashboard.getAvailableSlots() + split.availableSlots);
            dashboard.setOccupiedSlots(dashboard.getOccupiedSlots() + split.occupiedSlots);

            dashboard.getTwoWheelerSlots().setTotal(
                    dashboard.getTwoWheelerSlots().getTotal() + split.twoWheelerTotalSlots);
            dashboard.getTwoWheelerSlots().setAvailable(
                    dashboard.getTwoWheelerSlots().getAvailable() + split.twoWheelerAvailableSlots);
            dashboard.getTwoWheelerSlots().setOccupied(
                    dashboard.getTwoWheelerSlots().getOccupied() + split.twoWheelerOccupiedSlots);

            dashboard.getFourWheelerSlots().setTotal(
                    dashboard.getFourWheelerSlots().getTotal() + split.fourWheelerTotalSlots);
            dashboard.getFourWheelerSlots().setAvailable(
                    dashboard.getFourWheelerSlots().getAvailable() + split.fourWheelerAvailableSlots);
            dashboard.getFourWheelerSlots().setOccupied(
                    dashboard.getFourWheelerSlots().getOccupied() + split.fourWheelerOccupiedSlots);
        }

        dashboard.setLocations(locationSummaries);
        return okResponse("Vendor dashboard fetched successfully!", dashboard);
    }

    // Helper method
    private ParkingLocationResponseDto mapToResponse(ParkingLocation parking) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();
        dto.setId(parking.getId());
        dto.setName(parking.getName());
        dto.setAddress(parking.getAddress());
        dto.setLatitude(parking.getLatitude());
        dto.setLongitude(parking.getLongitude());
        dto.setTotalSlots(parking.getTotalSlots());
        dto.setAvailableSlots(parking.getAvailableSlots());
        dto.setTwoWheelerRatePerHour(parking.getTwoWheelerRatePerHour());
        dto.setFourWheelerRatePerHour(parking.getFourWheelerRatePerHour());
        dto.setVendorName(parking.getVendor().getName());
        return dto;
    }

    private ParkingLocation getVendorParkingLocationOrThrow(Long id, String email) {
        ParkingLocation parking = parkingLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking location not found"));

        if (parking.getVendor() == null || !parking.getVendor().getEmail().equals(email)) {
            throw new RuntimeException("You can only manage your own parking!");
        }

        return parking;
    }
//new one
    private SlotBreakdown splitSlots(int totalSlots, int availableSlots) {
        int safeTotalSlots = Math.max(totalSlots, 0);
        int safeAvailableSlots = Math.min(Math.max(availableSlots, 0), safeTotalSlots);

        int fourWheelerTotalSlots = safeTotalSlots / 2;
        int twoWheelerTotalSlots = safeTotalSlots - fourWheelerTotalSlots;

        int fourWheelerAvailableSlots = safeTotalSlots == 0
                ? 0
                : (int) Math.floor((double) safeAvailableSlots * fourWheelerTotalSlots / safeTotalSlots);
        int twoWheelerAvailableSlots = safeAvailableSlots - fourWheelerAvailableSlots;

        return new SlotBreakdown(
                safeTotalSlots,
                safeAvailableSlots,
                safeTotalSlots - safeAvailableSlots,
                twoWheelerTotalSlots,
                twoWheelerAvailableSlots,
                twoWheelerTotalSlots - twoWheelerAvailableSlots,
                fourWheelerTotalSlots,
                fourWheelerAvailableSlots,
                fourWheelerTotalSlots - fourWheelerAvailableSlots
        );
    }

    private record SlotBreakdown(
            int totalSlots,
            int availableSlots,
            int occupiedSlots,
            int twoWheelerTotalSlots,
            int twoWheelerAvailableSlots,
            int twoWheelerOccupiedSlots,
            int fourWheelerTotalSlots,
            int fourWheelerAvailableSlots,
            int fourWheelerOccupiedSlots) {
    }
}
