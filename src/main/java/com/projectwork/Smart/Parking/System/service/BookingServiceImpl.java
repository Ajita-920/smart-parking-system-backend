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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail) {

        User driver = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ParkingLocation location = parkingLocationRepository.findById(request.getParkingLocationId())
                .orElseThrow(() -> new RuntimeException("Parking location not found"));

        // === VALIDATION 1: Slot availability ===
        if (location.getAvailableSlots() <= 0) {
            throw new RuntimeException("No slots available at this location!");
        }

        // === VALIDATION 2: No overlapping booking (Double Booking Prevention) ===
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                location.getId(), request.getStartTime(), request.getEndTime());

        if (!overlaps.isEmpty()) {
            throw new RuntimeException("This time slot is already booked!");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setParkingLocation(location);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(calculateAmount(location)); // simple calculation

        Booking savedBooking = bookingRepository.save(booking);

        // Reduce available slots
        location.setAvailableSlots(location.getAvailableSlots() - 1);
        parkingLocationRepository.save(location);

        // Prepare response
        BookingResponseDto response = new BookingResponseDto();
        response.setBookingId(savedBooking.getId());
        response.setParkingName(location.getName());
        response.setStatus(savedBooking.getStatus());
        response.setStartTime(savedBooking.getStartTime());
        response.setEndTime(savedBooking.getEndTime());
        response.setTotalAmount(savedBooking.getTotalAmount());
        response.setMessage("Booking confirmed successfully!");

        return response;
    }

    @Override
    public Booking saveBooking(Booking booking) {
        return null;
    }

    @Override
    public List<BookingResponseDto> getMyBookings(String email) {
        User driver = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingRepository.findByDriver(driver);

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponseDto mapToResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingId(booking.getId());
        dto.setParkingName(booking.getParkingLocation().getName());
        dto.setStatus(booking.getStatus());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setTotalAmount(booking.getTotalAmount());
        return dto;
    }

    private double calculateAmount(ParkingLocation location) {
        return 100.0; // You can make it dynamic later (per hour, etc.)
    }
}