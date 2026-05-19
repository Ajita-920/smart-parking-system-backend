package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingCancelResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
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
    @Autowired
    private PaymentRepository paymentRepository;

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
        booking.setVehicleType(request.getVehicleType());
        booking.setTotalAmount(calculateAmount(location, request.getVehicleType(), request.getStartTime(), request.getEndTime()));
        booking.setUser(driver);
        booking.setDriver(driver);

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
        response.setVehicleType(savedBooking.getVehicleType());
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
        dto.setVehicleType(booking.getVehicleType());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setRefundAmount(booking.getRefundAmount());
        return dto;
    }
//new feature cancel added
    @Override
    @Transactional
    public BookingCancelResponseDto cancelBooking(Long bookingId, String email) {
        User driver = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingRepository.findByIdAndDriver(bookingId, driver)
                .orElseThrow(() -> new RuntimeException("Booking not found for current user"));

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "CANCELLED_REFUNDED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }

        booking.setCancelledAt(LocalDateTime.now());
        booking.setStatus("CANCELLED");
        booking.setRefundAmount(0.0);

        ParkingLocation location = booking.getParkingLocation();
        location.setAvailableSlots(Math.min(location.getTotalSlots(), location.getAvailableSlots() + 1));
        parkingLocationRepository.save(location);

        BookingCancelResponseDto response = new BookingCancelResponseDto();
        response.setBookingId(booking.getId());
        response.setStatus("CANCELLED");
        response.setRefunded(false);
        response.setRefundAmount(0.0);
        response.setMessage("Booking cancelled. Refund is not eligible.");

        boolean refundEligible = isRefundEligible(booking);
        if (refundEligible) {
            double refundAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : 0.0;
            booking.setStatus("CANCELLED_REFUNDED");
            booking.setRefundAmount(refundAmount);

            paymentRepository.findTopByBookingIdOrderByIdDesc(booking.getId()).ifPresent(payment -> {
                payment.setStatus("REFUNDED");
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            });

            response.setStatus("CANCELLED_REFUNDED");
            response.setRefunded(true);
            response.setRefundAmount(refundAmount);
            response.setMessage("Booking cancelled and full refund marked.");
        }

        bookingRepository.save(booking);
        return response;
    }

    private boolean isRefundEligible(Booking booking) {
        return booking.getStartTime() != null &&
                booking.getStartTime().isAfter(LocalDateTime.now().plusHours(1));
    }

    private double calculateAmount(ParkingLocation location, VehicleType vehicleType, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        long billableHours = Math.max(1, (long) Math.ceil(minutes / 60.0));

        double fallbackRate = 100.0;
        double ratePerHour;
        if (vehicleType == VehicleType.FOUR_WHEELER) {
            ratePerHour = location.getFourWheelerRatePerHour() != null ? location.getFourWheelerRatePerHour() : fallbackRate;
        } else {
            ratePerHour = location.getTwoWheelerRatePerHour() != null ? location.getTwoWheelerRatePerHour() : fallbackRate;
        }

        return ratePerHour * billableHours;
    }
}
