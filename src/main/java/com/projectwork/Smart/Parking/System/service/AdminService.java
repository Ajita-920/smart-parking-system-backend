package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;

import java.util.List;
import java.util.Map;

public interface AdminService {

    Map<String, Object> getDashboard();

    List<BookingResponseDto> getAllBookings();

    List<UserResponseDto> getUsers(String role);
}
