package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail);

    Booking saveBooking(Booking booking);
}