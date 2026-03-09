package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
    @RequestMapping("/api/parking")
    public class ParkingController {

        private final ParkingLocationRepository parkingRepository;

        public ParkingController(ParkingLocationRepository parkingRepository) {
            this.parkingRepository = parkingRepository;
        }

        @GetMapping("/available")
        public List<ParkingLocation> getAvailableParking() {
            return parkingRepository.findByAvailableSlotsGreaterThan(0);
        }

        @PostMapping("/add")
        public ParkingLocation addParking(@RequestBody ParkingLocation parking) {
            return parkingRepository.save(parking);
        }
    }

