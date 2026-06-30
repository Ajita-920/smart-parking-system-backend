package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.VendorBookingStatusRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.WalkInBookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingCancelResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Booking use cases for drivers and vendors.
 */
public interface BookingService {

    /**
     * Creates a booking for the current driver.
     */
    BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail);

    /**
     * Returns bookings visible to the current user.
     */
    List<BookingResponseDto> getMyBookings(String email);

    /**
     * Fetches a booking after applying ownership/role access rules.
     */
    BookingResponseDto getBookingById(UUID id, String currentUserEmail);

    /**
     * Cancels a driver's booking and releases the reserved slot when applicable.
     */
    BookingCancelResponseDto cancelBooking(UUID bookingId, String email);

    /**
     * Updates booking status from the vendor workflow.
     */
    BookingResponseDto updateVendorBookingStatus(
            UUID bookingId,
            VendorBookingStatusRequestDto request,
            String currentUserEmail);

    /**
     * Creates an immediate vendor walk-in booking for an available slot.
     */
    BookingResponseDto createWalkInBooking(WalkInBookingRequestDto request, String currentUserEmail);
}
