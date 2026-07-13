package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.SlotStatusUpdateRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.UpdateSlotsRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingSlotResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ParkingServiceImpl implements ParkingService {

    private final DijkstraService dijkstraService;
    private final BookingRepository bookingRepository;
    private final ParkingLocationRepository parkingLocationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public ParkingServiceImpl(
            DijkstraService dijkstraService,
            BookingRepository bookingRepository,
            ParkingLocationRepository parkingLocationRepository,
            ParkingSlotRepository parkingSlotRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.dijkstraService = dijkstraService;
        this.bookingRepository = bookingRepository;
        this.parkingLocationRepository = parkingLocationRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
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
    public List<ParkingLocationResponseDto> getNearbyParkingByRoadDistance(double latitude, double longitude, int limit) {
        return dijkstraService.findNearestByRoadDistance(latitude, longitude, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLocationResponseDto getSingleNearestParking(double latitude, double longitude) {
        return dijkstraService.findSingleNearestParking(latitude, longitude);
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

        if (!request.getTotalFourWheelerSlots().equals(parking.getTotalFourWheelerSlots())
                || !request.getTotalTwoWheelerSlots().equals(parking.getTotalTwoWheelerSlots())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Total slot counts cannot be changed while editing parking location details."
            );
        }

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

        if (bookingRepository.existsByParkingLocationAndStatusInAndDeletedAtIsNull(
                parking,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete parking location with active bookings."
            );
        }

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
                .map(slot -> toSlotResponseDto(slot, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlotResponseDto> getVendorSlots(UUID parkingLocationId, String currentUserEmail) {
        ParkingLocation parking = resolveOwnedParking(parkingLocationId, currentUserEmail);

        return parkingSlotRepository.findByLocationAndDeletedAtIsNull(parking)
                .stream()
                .map(slot -> toSlotResponseDto(slot, true))
                .toList();
    }

    @Override
    @Transactional
    public ParkingSlotResponseDto updateSlotStatus(
            UUID parkingLocationId,
            UUID slotId,
            SlotStatusUpdateRequestDto request,
            String currentUserEmail) {
        ParkingLocation parking = resolveOwnedParking(parkingLocationId, currentUserEmail);

        ParkingSlot slot = parkingSlotRepository.findByIdAndDeletedAtIsNullForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking slot not found."
                ));

        if (!slot.getLocation().getId().equals(parking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected slot does not belong to this parking location."
            );
        }

        ParkingSlotStatus requestedStatus = parseSlotStatus(request.getStatus());

        if (requestedStatus == ParkingSlotStatus.MAINTENANCE) {
            if (slot.getStatus() != ParkingSlotStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only available slots can be marked as maintenance."
                );
            }

            slot.markMaintenance();
            decrementAvailableSlotCount(parking, slot.getVehicleType());
        } else if (requestedStatus == ParkingSlotStatus.AVAILABLE) {
            if (slot.getStatus() != ParkingSlotStatus.MAINTENANCE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only maintenance slots can be marked as available."
                );
            }

            slot.markAvailable();
            incrementAvailableSlotCount(parking, slot.getVehicleType());
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid slot status transition."
            );
        }

        parkingLocationRepository.save(parking);
        ParkingSlot savedSlot = parkingSlotRepository.save(slot);
        return toSlotResponseDto(savedSlot, true);
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
            return DijkstraService.ThamelBoundary.isInThamel(
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

    private ParkingSlotStatus parseSlotStatus(String status) {
        try {
            return ParkingSlotStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid slot status. Allowed values: AVAILABLE, MAINTENANCE."
            );
        }
    }

    private void decrementAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
        if (vehicleType == VehicleType.FOUR_WHEELER) {
            location.setAvailableFourWheelerSlots(Math.max(0, location.getAvailableFourWheelerSlots() - 1));
        } else {
            location.setAvailableTwoWheelerSlots(Math.max(0, location.getAvailableTwoWheelerSlots() - 1));
        }
    }

    private void incrementAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
        if (vehicleType == VehicleType.FOUR_WHEELER) {
            location.setAvailableFourWheelerSlots(
                    Math.min(location.getTotalFourWheelerSlots(), location.getAvailableFourWheelerSlots() + 1));
        } else {
            location.setAvailableTwoWheelerSlots(
                    Math.min(location.getTotalTwoWheelerSlots(), location.getAvailableTwoWheelerSlots() + 1));
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

    private ParkingSlotResponseDto toSlotResponseDto(ParkingSlot slot, boolean includeActiveBooking) {
        ParkingSlotResponseDto dto = new ParkingSlotResponseDto();

        dto.setId(slot.getId());
        dto.setSlotNumber(slot.getSlotNumber());
        dto.setVehicleType(slot.getVehicleType());
        dto.setStatus(slot.getStatus());

        if (slot.getLocation() != null) {
            dto.setParkingLocationId(slot.getLocation().getId());
            dto.setParkingLocationName(slot.getLocation().getName());
        }

        if (includeActiveBooking && (slot.getStatus() == ParkingSlotStatus.RESERVED
                || slot.getStatus() == ParkingSlotStatus.BOOKED
                || slot.getStatus() == ParkingSlotStatus.OCCUPIED)) {
            BookingStatus activeStatus = slot.getStatus() == ParkingSlotStatus.RESERVED
                    ? BookingStatus.PENDING
                    : BookingStatus.CONFIRMED;
            bookingRepository.findFirstBySlotAndStatusAndDeletedAtIsNullOrderByStartTimeDesc(
                    slot,
                    activeStatus
            ).ifPresent(booking -> dto.setActiveBooking(toActiveBookingSummary(booking)));
        }

        return dto;
    }

    private ParkingSlotResponseDto.ActiveBookingSummaryDto toActiveBookingSummary(Booking booking) {
        ParkingSlotResponseDto.ActiveBookingSummaryDto dto =
                new ParkingSlotResponseDto.ActiveBookingSummaryDto();

        dto.setBookingId(booking.getId());
        if (booking.getDriver() != null) {
            dto.setDriverName(booking.getDriver().getName());
        }
        dto.setCustomerName(booking.getCustomerName());
        dto.setCustomerPhone(booking.getCustomerPhone());
        dto.setVehicleNumber(booking.getVehicleNumber());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setStatus(booking.getStatus());
        dto.setTotalAmount(booking.getTotalAmount());

        paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                booking.getId()
        ).ifPresent(payment -> applyPaymentSummary(dto, payment));

        return dto;
    }

    private void applyPaymentSummary(
            ParkingSlotResponseDto.ActiveBookingSummaryDto dto,
            Payment payment
    ) {
        dto.setPaymentStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
    }
}
