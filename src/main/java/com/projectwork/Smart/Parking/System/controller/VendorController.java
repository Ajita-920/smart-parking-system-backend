package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendor")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('VENDOR')")   // Only VENDOR role can access these endpoints
public class VendorController {

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    @Autowired
    private UserRepository userRepository;

    // ADD NEW PARKING LOCATION ====================
    @PostMapping("/parking")
    public ResponseEntity<ApiResponse> addParkingLocation(
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

        return ResponseEntity.ok(new ApiResponse("Parking location added successfully!", mapToResponse(saved)));
    }

    // GET MY PARKING LOCATIONS
    @GetMapping("/parking/my")
    public ResponseEntity<ApiResponse> getMyParkingLocations(Authentication authentication) {
        String email = authentication.getName();
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        List<ParkingLocation> parkings = parkingLocationRepository.findByVendor(vendor);

        List<ParkingLocationResponseDto> responseList = parkings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse("My parking locations fetched!", responseList));
    }

    // ==================== UPDATE AVAILABLE SLOTS ====================
    @PutMapping("/parking/{id}/slots")
    public ResponseEntity<ApiResponse> updateAvailableSlots(
            @PathVariable Long id,
            @RequestParam int newAvailableSlots,
            Authentication authentication) {

        ParkingLocation parking = parkingLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parking location not found"));

        // Security check: Only owner vendor can update
        String email = authentication.getName();
        if (!parking.getVendor().getEmail().equals(email)) {
            throw new RuntimeException("You can only update your own parking!");
        }

        parking.setAvailableSlots(newAvailableSlots);
        parkingLocationRepository.save(parking);

        return ResponseEntity.ok(new ApiResponse("Available slots updated successfully!", null));
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
}