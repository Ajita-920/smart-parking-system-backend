package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.service.DijkstraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstant.PARKING_BASE)
public class ParkingController extends BaseController {

    private final DijkstraService dijkstraService;
    private final ParkingLocationRepository parkingLocationRepository;
    private final UserRepository userRepository;

    public ParkingController(DijkstraService dijkstraService,
                             ParkingLocationRepository parkingLocationRepository,
                             UserRepository userRepository) {
        this.dijkstraService = dijkstraService;
        this.parkingLocationRepository = parkingLocationRepository;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC / AUTH READ ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/parking?area=thamel&available=true
     *
     * Query params:
     *   area      (optional) — filter by named area, e.g. "thamel"
     *   available (optional, default true) — only show slots with available > 0
     *
     * Returns all parking locations matching the filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getAllParking(
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "true") boolean available) {

        List<ParkingLocation> locations = available
                ? parkingLocationRepository.findByAvailableSlotsGreaterThan(0)
                : parkingLocationRepository.findAll();

        List<ParkingLocationResponseDto> dtos = locations.stream()
                .filter(loc -> area == null || matchesArea(area, loc))
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            return okResponse("No parking locations found.", dtos);
        }
        return okResponse("Parking locations fetched successfully!", dtos);
    }

    /**
     * GET /api/parking/nearby?lat=27.71&lng=85.31&limit=5
     *
     * Query params:
     *   lat   (required) — user latitude
     *   lng   (required) — user longitude
     *   limit (optional, default 5) — max results
     *
     * Returns up to `limit` parking spots sorted by road distance (Dijkstra).
     */
    @GetMapping(ApiConstant.PARKING_NEARBY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getNearbyParking(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int limit) {

        List<ParkingLocationResponseDto> spots = dijkstraService.findClosestInThamel(lat, lng, limit);

        if (spots.isEmpty()) {
            return okResponse("No parking spots found near your location.", spots);
        }
        return okResponse("Found " + spots.size() + " nearby parking spots.", spots);
    }

    /**
     * GET /api/parking/nearest?lat=27.71&lng=85.31
     *
     * Query params:
     *   lat (required) — user latitude
     *   lng (required) — user longitude
     *
     * Returns the single nearest parking spot by road distance.
     */
    @GetMapping(ApiConstant.PARKING_NEAREST)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> getNearestParking(
            @RequestParam double lat,
            @RequestParam double lng) {

        ParkingLocationResponseDto nearest = dijkstraService.findNearestParking(lat, lng);

        if (nearest == null) {
            return okResponse("No parking spots found near your location.", null);
        }
        return okResponse("Nearest parking spot found!", nearest);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VENDOR ENDPOINTS  (VENDOR role required)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/parking/mine
     * Returns all parking locations owned by the authenticated vendor.
     */
    @GetMapping(ApiConstant.PARKING_MINE)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponseDto>>> getMyParkingLocations(
            Authentication authentication) {

        User vendor = resolveVendor(authentication);
        List<ParkingLocationResponseDto> dtos = parkingLocationRepository.findByVendor(vendor)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        return okResponse("Your parking locations fetched successfully!", dtos);
    }

    /**
     * POST /api/parking
     * Body: ParkingLocationRequestDto
     * Creates a new parking location owned by the authenticated vendor.
     */
    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> addParkingLocation(
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {

        User vendor = resolveVendor(authentication);

        if (request.getTotalSlots() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total slots must be greater than zero.");
        }

        ParkingLocation parking = new ParkingLocation();
        parking.setName(request.getName());
        parking.setAddress(request.getAddress());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());
        parking.setTotalSlots(request.getTotalSlots());
        parking.setAvailableSlots(request.getTotalSlots()); // starts fully available
        parking.setVendor(vendor);

        ParkingLocation saved = parkingLocationRepository.save(parking);
        return okResponse("Parking location added successfully!", toResponseDto(saved));
    }

    /**
     * PUT /api/parking/{id}
     * Body: ParkingLocationRequestDto
     * Full update of a parking location. Vendor must own the location.
     */
    @PutMapping(ApiConstant.PARKING_BY_ID)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> updateParkingLocation(
            @PathVariable Long id,
            @Valid @RequestBody ParkingLocationRequestDto request,
            Authentication authentication) {

        ParkingLocation parking = resolveOwnedParking(id, authentication);

        if (request.getTotalSlots() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total slots must be greater than zero.");
        }

        parking.setName(request.getName());
        parking.setAddress(request.getAddress());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());
        parking.setTotalSlots(request.getTotalSlots());
        // Clamp available slots to new total if needed
        if (parking.getAvailableSlots() > request.getTotalSlots()) {
            parking.setAvailableSlots(request.getTotalSlots());
        }

        ParkingLocation saved = parkingLocationRepository.save(parking);
        return okResponse("Parking location updated successfully!", toResponseDto(saved));
    }

    /**
     * PATCH /api/parking/{id}/slots
     * Body: UpdateSlotsRequestDto { availableSlots: int }
     * Partial update — only changes available slot count.
     * Vendor must own the location.
     */
    @PatchMapping(ApiConstant.PARKING_SLOTS)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ParkingLocationResponseDto>> updateAvailableSlots(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSlotsRequestDto request,
            Authentication authentication) {

        ParkingLocation parking = resolveOwnedParking(id, authentication);

        int newSlots = request.getAvailableSlots();
        if (newSlots < 0 || newSlots > parking.getTotalSlots()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "availableSlots must be between 0 and " + parking.getTotalSlots() + ".");
        }

        parking.setAvailableSlots(newSlots);
        ParkingLocation saved = parkingLocationRepository.save(parking);
        return okResponse("Available slots updated successfully!", toResponseDto(saved));
    }

    /**
     * DELETE /api/parking/{id}
     * Removes a parking location. Vendor must own the location.
     */
    @DeleteMapping(ApiConstant.PARKING_BY_ID)
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteParkingLocation(
            @PathVariable Long id,
            Authentication authentication) {

        ParkingLocation parking = resolveOwnedParking(id, authentication);
        parkingLocationRepository.delete(parking);
        return okResponse("Parking location deleted successfully!", null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Resolves the authenticated user as a Vendor entity. */
    private User resolveVendor(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found."));
    }

    /**
     * Fetches the parking location and verifies the authenticated vendor owns it.
     * Throws 404 if not found, 403 if owned by another vendor.
     */
    private ParkingLocation resolveOwnedParking(Long id, Authentication authentication) {
        ParkingLocation parking = parkingLocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking location not found."));

        String email = authentication.getName();
        if (!parking.getVendor().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this parking location.");
        }
        return parking;
    }

    /** Filter helper: checks whether a location belongs to the named area. */
    private boolean matchesArea(String area, ParkingLocation loc) {
        if ("thamel".equalsIgnoreCase(area)) {
            return DijkstraService.AreaRestriction.isInThamel(loc.getLatitude(), loc.getLongitude());
        }
        // extend here for other areas in future
        return true;
    }

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