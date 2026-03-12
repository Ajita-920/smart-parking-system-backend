package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.service.algorithm.DijkstraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    @Autowired
    private DijkstraService dijkstraService;

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    // Search nearest parking using Dijkstra
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse> findNearbyParking(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(latitude, longitude);

        return ResponseEntity.ok(new ApiResponse("Nearest parking found successfully!", nearest));
    }
    @GetMapping("/slots")
    @PreAuthorize("isAuthenticated()")   // or remove if you want it public / no login needed
    public ResponseEntity<ApiResponse> getParkingSlots() {
        // Assuming you have ParkingSlotRepository or ParkingLocationRepository
        // Adjust based on your actual entity (ParkingLocation or ParkingSlot?)
        List<ParkingLocation> slots = parkingLocationRepository.findAll();   // or findByAvailableSlotsGreaterThan(0)

        List<ParkingLocationResponseDto> dtos = slots.stream()
                .map(this::toResponseDto)   // reuse your mapper or create one
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse("Parking slots retrieved", dtos));
    }

    // Add this helper if not present
    private ParkingLocationResponseDto toResponseDto(ParkingLocation loc) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();
        dto.setId(loc.getId());
        dto.setName(loc.getName());
        dto.setAddress(loc.getAddress());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setAvailableSlots(loc.getAvailableSlots());
        // add other fields
        return dto;
    }
}