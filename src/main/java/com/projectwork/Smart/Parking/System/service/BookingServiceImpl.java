package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {

    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("100.00");

    private final BookingRepository bookingRepository;
    private final ParkingLocationRepository parkingLocationRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            ParkingLocationRepository parkingLocationRepository,
            ParkingSlotRepository parkingSlotRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.parkingLocationRepository = parkingLocationRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail) {

        User driver = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only drivers can create bookings."
            );
        }

        ParkingLocation location = parkingLocationRepository
                .findByIdAndDeletedAtIsNull(request.getParkingLocationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parking location not found."
                ));

        VehicleType vehicleType = parseVehicleType(request.getVehicleType());

        validateAvailableSlotCount(location, vehicleType);

        ParkingSlot slot = parkingSlotRepository
                .findByLocation_IdAndVehicleTypeAndStatusAndDeletedAtIsNull(
                        location.getId(),
                        vehicleType,
                        ParkingSlotStatus.AVAILABLE
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No available " + vehicleType + " slot at this location."
                ));

        slot.markReserved();
        decrementAvailableSlotCount(location, vehicleType);

        Booking booking = new Booking();
        booking.setDriver(driver);
        booking.setParkingLocation(location);
        booking.setSlot(slot);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(calculateAmount(request.getStartTime(), request.getEndTime()));

        ParkingSlot savedSlot = parkingSlotRepository.save(slot);
        ParkingLocation savedLocation = parkingLocationRepository.save(location);

        booking.setSlot(savedSlot);
        booking.setParkingLocation(savedLocation);

        Booking savedBooking = bookingRepository.save(booking);

        BookingResponseDto response = mapToResponse(savedBooking);
        response.setMessage("Booking created successfully.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(String currentUserEmail) {
        User driver = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        return bookingRepository.findByDriverAndDeletedAtIsNull(driver)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(UUID id, String currentUserEmail) {
        Booking booking = bookingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found."
                ));

        User currentUser = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isBookingDriver = booking.getDriver().getId().equals(currentUser.getId());
        boolean isLocationVendor = currentUser.getRole() == UserRole.VENDOR
                && booking.getParkingLocation().getVendor().getId().equals(currentUser.getId());

        if (!isAdmin && !isBookingDriver && !isLocationVendor) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to view this booking."
            );
        }

        BookingResponseDto response = mapToResponse(booking);
        response.setMessage("Booking fetched successfully.");
        return response;
    }

    private BookingResponseDto mapToResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();

        dto.setBookingId(booking.getId());

        dto.setDriverId(booking.getDriver().getId());
        dto.setDriverName(booking.getDriver().getName());

        dto.setParkingLocationId(booking.getParkingLocation().getId());
        dto.setParkingLocationName(booking.getParkingLocation().getName());

        dto.setSlotId(booking.getSlot().getId());
        dto.setSlotNumber(booking.getSlot().getSlotNumber());
        dto.setVehicleType(booking.getSlot().getVehicleType());

        dto.setStatus(booking.getStatus());

        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());

        dto.setTotalAmount(booking.getTotalAmount());

        return dto;
    }

    private BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));

        return DEFAULT_HOURLY_RATE.multiply(BigDecimal.valueOf(hours));
    }

    private void validateAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
        if (vehicleType == VehicleType.FOUR_WHEELER && location.getAvailableFourWheelerSlots() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No available four-wheeler slots at this location."
            );
        }

        if (vehicleType == VehicleType.TWO_WHEELER && location.getAvailableTwoWheelerSlots() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No available two-wheeler slots at this location."
            );
        }
    }

    private void decrementAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
        if (vehicleType == VehicleType.FOUR_WHEELER) {
            location.setAvailableFourWheelerSlots(location.getAvailableFourWheelerSlots() - 1);
        } else {
            location.setAvailableTwoWheelerSlots(location.getAvailableTwoWheelerSlots() - 1);
        }
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}