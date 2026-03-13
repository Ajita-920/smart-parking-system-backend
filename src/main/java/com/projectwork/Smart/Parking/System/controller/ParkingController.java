package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.service.DijkstraService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    private final DijkstraService dijkstraService;
    private final ParkingLocationRepository parkingLocationRepository;

    public ParkingController(DijkstraService dijkstraService, ParkingLocationRepository parkingLocationRepository) {
        this.dijkstraService = dijkstraService;
        this.parkingLocationRepository = parkingLocationRepository;
    }

 //closest parking
    @GetMapping("/thamel-nearby")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> findClosestInThamel(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int maxSpots) {

        List<ParkingLocationResponseDto> spots =
                dijkstraService.findClosestInThamel(latitude, longitude, maxSpots);

        if (spots.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse("No parking spots available in Thamel.", null));
        }

        return ResponseEntity.ok(new ApiResponse(
                "Found " + spots.size() + " parking spots in Thamel sorted by road distance",
                spots
        ));
    }

  //near one
    @GetMapping("/nearby")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> findNearestInThamel(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(latitude, longitude);

        if (nearest == null) {
            return ResponseEntity.ok(new ApiResponse("No parking spots available in Thamel.", null));
        }

        return ResponseEntity.ok(new ApiResponse(
                "Nearest parking in Thamel found successfully!",
                nearest
        ));
    }


//sabbai
    @GetMapping("/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getAllInThamel() {
        List<ParkingLocation> locations = parkingLocationRepository.findByAvailableSlotsGreaterThan(0);

        List<ParkingLocationResponseDto> dtos = locations.stream()
                .filter(loc -> DijkstraService.AreaRestriction.isInThamel(loc.getLatitude(), loc.getLongitude()))
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse("No available parking in Thamel.", null));
        }

        return ResponseEntity.ok(new ApiResponse("All available parking in Thamel", dtos));
    }

  //dto

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