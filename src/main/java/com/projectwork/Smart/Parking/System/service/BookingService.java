package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.VendorBookingStatusRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingCancelResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail);

    List<BookingResponseDto> getMyBookings(String email);

    BookingResponseDto getBookingById(UUID id, String currentUserEmail);

    BookingCancelResponseDto cancelBooking(UUID bookingId, String email);

    BookingResponseDto updateVendorBookingStatus(
            UUID bookingId,
            VendorBookingStatusRequestDto request,
            String currentUserEmail);
}
