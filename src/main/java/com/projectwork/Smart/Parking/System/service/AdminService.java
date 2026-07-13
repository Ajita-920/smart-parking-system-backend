package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-facing business operations.
 */
public interface AdminService {

    /**
     * Builds aggregate metrics for the admin dashboard.
     */
    Map<String, Object> getDashboard();

    /**
     * Returns all active bookings for admin review.
     */
    List<BookingResponseDto> getAllBookings();

    /**
     * Returns active users, optionally filtered by role.
     */
    List<UserResponseDto> getUsers(String role);

    /**
     * Prevents a non-admin user from authenticating.
     */
    UserResponseDto banUser(UUID id);

    /**
     * Allows a previously banned non-admin user to authenticate again.
     */
    UserResponseDto unbanUser(UUID id);

    /**
     * Soft-deletes a non-admin user.
     */
    void deleteUser(UUID id);

    /**
     * Allows a vendor account to authenticate and use vendor features.
     */
    UserResponseDto approveVendor(UUID id);

    /**
     * Soft-deletes a vendor and related parking locations.
     */
    void deleteVendor(UUID id);
}
