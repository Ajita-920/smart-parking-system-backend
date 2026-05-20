package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminService {

    Map<String, Object> getDashboard();

    List<BookingResponseDto> getAllBookings();

    List<UserResponseDto> getUsers(String role);

    UserResponseDto banUser(UUID id);

    void deleteUser(UUID id);

    UserResponseDto approveVendor(UUID id);

    void deleteVendor(UUID id);
}
