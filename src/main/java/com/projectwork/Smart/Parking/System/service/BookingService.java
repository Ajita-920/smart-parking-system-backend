package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    /** Create a new booking for the given user email. */
    BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail);

    /** Fetch all bookings belonging to the given user email. */
    List<BookingResponseDto> getMyBookings(String email);

    /**
     * Fetch a single booking by ID.
     * Enforces ownership: a DRIVER can only fetch their own booking.
     * ADMIN bypass should be handled in the implementation via role check.
     */
    BookingResponseDto getBookingById(UUID id, String currentUserEmail);
}