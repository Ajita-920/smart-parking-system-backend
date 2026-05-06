package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
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

        ParkingLocation parking = new ParkingLocation();
        parking.setName(request.getName());
        parking.setAddress(request.getAddress());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());
        parking.setTotalSlots(request.getTotalSlots());
        parking.setAvailableSlots(request.getTotalSlots());
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
    @PutMapping({ApiConstant.VENDOR_UPDATE_PARKING_AVAILABLE_SLOTS, ApiConstant.VENDOR_UPDATE_PARKING_LEGACY})
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
        dto.setAvailableSlots(parking.getAvailableSlots());
        dto.setVendorName(parking.getVendor().getName());
        return dto;
    }

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
