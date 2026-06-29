package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;

public interface EmailService {

    void sendBookingConfirmation(BookingResponseDto booking, String toEmail);
}
