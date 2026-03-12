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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getParkingSlots(
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon) {

        List<ParkingLocation> locations = parkingLocationRepository.findByAvailableSlotsGreaterThan(0);

        List<ParkingLocationResponseDto> dtos = locations.stream()
                .map(loc -> {
                    ParkingLocationResponseDto dto = toResponseDto(loc);
                    if (userLat != null && userLon != null) {
                        dto.setDistance(calculateDistance(userLat, userLon, loc.getLatitude(), loc.getLongitude()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse("Available parking locations retrieved", dtos));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(R * c * 100.0) / 100.0; // 2 decimal places
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
        dto.setVendorName(loc.getVendor() != null ? loc.getVendor().getName() : "Unknown");
        return dto;
    }
}