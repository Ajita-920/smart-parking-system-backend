package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingSlotResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements parking search, vendor ownership checks, slot creation, and slot
 * availability updates.
 */
@Service
public class ParkingServiceImpl implements ParkingService {

    private final DijkstraService dijkstraService;
    private final ParkingLocationRepository parkingLocationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final UserRepository userRepository;

    public ParkingServiceImpl(
            DijkstraService dijkstraService,
            ParkingLocationRepository parkingLocationRepository,
            ParkingSlotRepository parkingSlotRepository,
            UserRepository userRepository
    ) {
        this.dijkstraService = dijkstraService;
        this.parkingLocationRepository = parkingLocationRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLocationResponseDto> getAllParking(String area, boolean available) {
        return parkingLocationRepository.findByDeletedAtIsNull()
                .stream()
                .filter(location -> !available || location.getAvailableSlots() > 0)
                .filter(location -> area == null || matchesArea(area, location))
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLocationResponseDto> getNearbyParking(double latitude, double longitude, int limit) {
        return dijkstraService.findClosestInThamel(latitude, longitude, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLocationResponseDto getNearestParking(double latitude, double longitude) {
        return dijkstraService.findNearestParking(latitude, longitude);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLocationResponseDto getParkingById(UUID id) {
        ParkingLocation parking = parkingLocationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking location not found."
                ));

        return toResponseDto(parking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLocationResponseDto> getMyParkingLocations(String currentUserEmail) {
        User vendor = resolveVendor(currentUserEmail);

        return parkingLocationRepository.findByVendorAndDeletedAtIsNull(vendor)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public ParkingLocationResponseDto addParkingLocation(
            ParkingLocationRequestDto request,
            String currentUserEmail
    ) {
        User vendor = resolveVendor(currentUserEmail);

        int totalFourWheelerSlots = request.getTotalFourWheelerSlots();
        int totalTwoWheelerSlots = request.getTotalTwoWheelerSlots();

        if (totalFourWheelerSlots + totalTwoWheelerSlots <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Total slots must be greater than zero."
            );
        }

        if (parkingLocationRepository.existsByNameIgnoreCaseAndVendorAndDeletedAtIsNull(
                request.getName().trim(),
                vendor
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a parking location with this name."
            );
        }

        ParkingLocation parking = new ParkingLocation();
        parking.setName(request.getName().trim());
        parking.setAddress(request.getAddress().trim());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());

        parking.setTotalFourWheelerSlots(totalFourWheelerSlots);
        parking.setAvailableFourWheelerSlots(totalFourWheelerSlots);

        parking.setTotalTwoWheelerSlots(totalTwoWheelerSlots);
        parking.setAvailableTwoWheelerSlots(totalTwoWheelerSlots);

        parking.setFourWheelerRatePerHour(request.getFourWheelerRatePerHour());
        parking.setTwoWheelerRatePerHour(request.getTwoWheelerRatePerHour());

        parking.setVendor(vendor);

        ParkingLocation savedLocation = parkingLocationRepository.save(parking);

        createSlots(savedLocation, VehicleType.FOUR_WHEELER, totalFourWheelerSlots, "FW");
        createSlots(savedLocation, VehicleType.TWO_WHEELER, totalTwoWheelerSlots, "TW");

        return toResponseDto(savedLocation);
    }

    @Override
    @Transactional
    public ParkingLocationResponseDto updateParkingLocation(
            UUID id,
            ParkingLocationRequestDto request,
            String currentUserEmail
    ) {
        ParkingLocation parking = resolveOwnedParking(id, currentUserEmail);

        parking.setName(request.getName().trim());
        parking.setAddress(request.getAddress().trim());
        parking.setLatitude(request.getLatitude());
        parking.setLongitude(request.getLongitude());
        parking.setFourWheelerRatePerHour(request.getFourWheelerRatePerHour());
        parking.setTwoWheelerRatePerHour(request.getTwoWheelerRatePerHour());

        /*
         * This updates only location details.
         * Do not change total slot counts here unless you also add/remove ParkingSlot rows.
         * Use a dedicated slot-management service when you want proper slot resizing.
         */

        ParkingLocation saved = parkingLocationRepository.save(parking);
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public ParkingLocationResponseDto updateAvailableSlots(
            UUID id,
            UpdateSlotsRequestDto request,
            String currentUserEmail
    ) {
        ParkingLocation parking = resolveOwnedParking(id, currentUserEmail);

        if (request.getAvailableFourWheelerSlots() > parking.getTotalFourWheelerSlots()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Available four-wheeler slots cannot be greater than total four-wheeler slots."
            );
        }

        if (request.getAvailableTwoWheelerSlots() > parking.getTotalTwoWheelerSlots()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Available two-wheeler slots cannot be greater than total two-wheeler slots."
            );
        }

        parking.setAvailableFourWheelerSlots(request.getAvailableFourWheelerSlots());
        parking.setAvailableTwoWheelerSlots(request.getAvailableTwoWheelerSlots());

        ParkingLocation saved = parkingLocationRepository.save(parking);
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteParkingLocation(UUID id, String currentUserEmail) {
        ParkingLocation parking = resolveOwnedParking(id, currentUserEmail);
        parking.softDelete();
        parkingLocationRepository.save(parking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlotResponseDto> getParkingSlots(UUID parkingLocationId, String vehicleType) {
        ParkingLocation parking = parkingLocationRepository.findByIdAndDeletedAtIsNull(parkingLocationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking location not found."
                ));

        List<ParkingSlot> slots;

        slots = parkingSlotRepository.findByLocationAndDeletedAtIsNull(parking);

        if (vehicleType != null && !vehicleType.isBlank()) {
            VehicleType parsedVehicleType = parseVehicleType(vehicleType);
            slots = slots.stream()
                    .filter(slot -> slot.getVehicleType() == parsedVehicleType)
                    .toList();
        }

        return slots
                .stream()
                .map(this::toSlotResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlotResponseDto> getVendorSlots(UUID parkingLocationId, String currentUserEmail) {
        ParkingLocation parking = resolveOwnedParking(parkingLocationId, currentUserEmail);

        return parkingSlotRepository.findByLocationAndDeletedAtIsNull(parking)
                .stream()
                .map(this::toSlotResponseDto)
                .toList();
    }

    private User resolveVendor(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vendor not found."
                ));
    }

    private ParkingLocation resolveOwnedParking(UUID id, String currentUserEmail) {
        ParkingLocation parking = parkingLocationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking location not found."
                ));

        if (!parking.getVendor().getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this parking location."
            );
        }

        return parking;
    }

    private void createSlots(
            ParkingLocation location,
            VehicleType vehicleType,
            int totalSlots,
            String prefix
    ) {
        List<ParkingSlot> slots = new ArrayList<>();

        for (int i = 1; i <= totalSlots; i++) {
            ParkingSlot slot = new ParkingSlot();
            slot.setLocation(location);
            slot.setVehicleType(vehicleType);
            slot.setStatus(ParkingSlotStatus.AVAILABLE);
            slot.setSlotNumber(prefix + "-" + i);
            slots.add(slot);
        }

        parkingSlotRepository.saveAll(slots);
    }

    private boolean matchesArea(String area, ParkingLocation location) {
        if ("thamel".equalsIgnoreCase(area)) {
            return DijkstraService.AreaRestriction.isInThamel(
                    location.getLatitude(),
                    location.getLongitude()
            );
        }

        return true;
    }

    private VehicleType parseVehicleType(String vehicleType) {
        try {
            return VehicleType.valueOf(vehicleType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid vehicle type. Allowed values: TWO_WHEELER, FOUR_WHEELER."
            );
        }
    }

    private ParkingLocationResponseDto toResponseDto(ParkingLocation location) {
        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();

        dto.setId(location.getId());

        dto.setName(location.getName());
        dto.setAddress(location.getAddress());

        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());

        dto.setTotalFourWheelerSlots(location.getTotalFourWheelerSlots());
        dto.setAvailableFourWheelerSlots(location.getAvailableFourWheelerSlots());

        dto.setTotalTwoWheelerSlots(location.getTotalTwoWheelerSlots());
        dto.setAvailableTwoWheelerSlots(location.getAvailableTwoWheelerSlots());

        dto.setTotalSlots(location.getTotalSlots());
        dto.setAvailableSlots(location.getAvailableSlots());
        dto.setFourWheelerRatePerHour(location.getFourWheelerRatePerHour());
        dto.setTwoWheelerRatePerHour(location.getTwoWheelerRatePerHour());

        if (location.getVendor() != null) {
            dto.setVendorId(location.getVendor().getId());
            dto.setVendorName(location.getVendor().getName());
        }

        return dto;
    }

    private ParkingSlotResponseDto toSlotResponseDto(ParkingSlot slot) {
        ParkingSlotResponseDto dto = new ParkingSlotResponseDto();

        dto.setId(slot.getId());
        dto.setSlotNumber(slot.getSlotNumber());
        dto.setVehicleType(slot.getVehicleType());
        dto.setStatus(slot.getStatus());

        if (slot.getLocation() != null) {
            dto.setParkingLocationId(slot.getLocation().getId());
            dto.setParkingLocationName(slot.getLocation().getName());
        }

        return dto;
    }
}
