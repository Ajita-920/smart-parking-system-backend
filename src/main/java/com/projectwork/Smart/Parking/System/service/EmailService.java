package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.User;

public interface EmailService {

    void sendBookingConfirmation(BookingResponseDto booking, String toEmail);

    void sendVendorApprovalEmail(User vendor);
}
