package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail) {

        User driver = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        ParkingLocation location = parkingLocationRepository.findById(request.getParkingLocationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking location not found."));

        if (location.getAvailableSlots() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No available slots at this location.");
        }

        // Double-booking prevention: reject overlapping reservations for the same location
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                location.getId(), request.getStartTime(), request.getEndTime());

        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This time slot is already booked at the selected location.");
        }

        Booking booking = new Booking();
        booking.setDriver(driver);
        booking.setParkingLocation(location);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(calculateAmount(location));

        Booking saved = bookingRepository.save(booking);

        // Decrement available slot count
        location.setAvailableSlots(location.getAvailableSlots() - 1);
        parkingLocationRepository.save(location);

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<BookingResponseDto> getMyBookings(String email) {
        User driver = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        return bookingRepository.findByDriver(driver)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public BookingResponseDto getBookingById(Long id, String currentUserEmail) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking with ID " + id + " not found."));

        // Ownership check: the requester must be the booking's driver
        // (ADMIN bypass: check role in the security layer via @PreAuthorize, not here)
        if (!booking.getDriver().getEmail().equals(currentUserEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to view this booking.");
        }

        return mapToResponse(booking);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private BookingResponseDto mapToResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingId(booking.getId());
        dto.setParkingName(booking.getParkingLocation().getName());
        dto.setStatus(booking.getStatus());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setMessage("Booking fetched successfully.");
        return dto;
    }

    private double calculateAmount(ParkingLocation location) {
        // TODO: make dynamic — e.g. (duration in hours) * (rate per hour from ParkingLocation)
        return 100.0;
    }
}