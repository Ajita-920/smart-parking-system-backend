package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.service.algorithm.DijkstraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    @Autowired
    private DijkstraService dijkstraService;

    // Search nearest parking using Dijkstra
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse> findNearbyParking(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(latitude, longitude);

        return ResponseEntity.ok(new ApiResponse("Nearest parking found successfully!", nearest));
    }
}