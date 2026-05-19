package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
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
@RequestMapping(ApiConstant.PARKING_BASE)
@CrossOrigin(origins = "*")
public class ParkingController extends BaseController{

    private final DijkstraService dijkstraService;
    private final ParkingLocationRepository parkingLocationRepository;

    public ParkingController(DijkstraService dijkstraService, ParkingLocationRepository parkingLocationRepository) {
        this.dijkstraService = dijkstraService;
        this.parkingLocationRepository = parkingLocationRepository;
    }

 //closest parking
 @GetMapping({ApiConstant.PARKING_THAMEL_CLOSEST})
 @PreAuthorize("isAuthenticated()")
 public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> findClosestInThamel(
         @RequestParam double latitude,
         @RequestParam double longitude,
         @RequestParam(defaultValue = "5") int maxSpots) {
     List<ParkingLocationResponseDto> spots =
             dijkstraService.findClosestInThamel(latitude, longitude, maxSpots);
     if (spots.isEmpty()) {
         return okResponse("No parking spots available in Thamel.", null);
     }
     return okResponse("Found " + spots.size() + " parking spots in Thamel sorted by road distance",spots);
 }
//new one
    // Real GPS nearby sorting (Haversine distance)
    @GetMapping(ApiConstant.PARKING_NEARBY_GPS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> findClosestByGps(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") int maxSpots) {
        List<ParkingLocationResponseDto> spots = dijkstraService.findClosestByGps(latitude, longitude, maxSpots);
        if (spots.isEmpty()) {
            return okResponse("No parking spots available.", null);
        }
        return okResponse("Found " + spots.size() + " parking spots sorted by GPS distance", spots);
    }
//new one
  //near one
  @GetMapping({ApiConstant.PARKING_THAMEL_NEAREST})
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> findNearestInThamel(
          @RequestParam double latitude,
          @RequestParam double longitude) {
      ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(latitude, longitude);

      if (nearest == null) {
          return okResponse("No parking spots available in Thamel.", null);
      }
      return okResponse( "Nearest parking in Thamel found successfully!", nearest);
  }


//sabbai
    @GetMapping({ApiConstant.PARKING_THAMEL_AVAILABLE_SLOTS})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getAllInThamel() {
        List<ParkingLocation> locations = parkingLocationRepository.findByAvailableSlotsGreaterThan(0);
        List<ParkingLocationResponseDto> dtos = locations.stream()
                .filter(loc -> DijkstraService.AreaRestriction.isInThamel(loc.getLatitude(), loc.getLongitude()))
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        if (dtos.isEmpty()) {
            return okResponse("No available parking in Thamel.", null);
        }
        return okResponse("All available parking slots in Thamel", dtos);
    }

    @GetMapping(ApiConstant.PARKING_MAP_SPACES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getAllParkingForMap() {
        List<ParkingLocationResponseDto> dtos = parkingLocationRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return okResponse("All parking spaces for map fetched successfully", dtos);
    }

  //dto

    private ParkingLocationResponseDto toResponseDto(ParkingLocation loc) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();
        dto.setId(loc.getId());
        dto.setName(loc.getName());
        dto.setAddress(loc.getAddress());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setTotalSlots(loc.getTotalSlots());
        dto.setAvailableSlots(loc.getAvailableSlots());
        dto.setTwoWheelerRatePerHour(loc.getTwoWheelerRatePerHour());
        dto.setFourWheelerRatePerHour(loc.getFourWheelerRatePerHour());
        dto.setVendorName(loc.getVendor() != null ? loc.getVendor().getName() : "Unknown");
        return dto;
    }
}
