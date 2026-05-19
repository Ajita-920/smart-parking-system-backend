package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstant.ADMIN_BASE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends BaseController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/admin/dashboard
     * Returns platform-wide summary statistics.
     */
    @GetMapping(ApiConstant.ADMIN_DASHBOARD)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", bookingRepository.count());
        stats.put("totalVendors",  userRepository.countByRole("VENDOR"));
        stats.put("totalDrivers",  userRepository.countByRole("DRIVER"));

        return okResponse("Admin dashboard fetched successfully!", stats);
    }

    /**
     * GET /api/admin/bookings
     * Returns all bookings in the system.
     */
    @GetMapping(ApiConstant.ADMIN_BOOKINGS)
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return okResponse("All bookings fetched successfully!", bookingRepository.findAll());
    }

    /**
     * GET /api/admin/users?role=VENDOR
     * GET /api/admin/users?role=DRIVER
     * GET /api/admin/users           (returns all users)
     *
     * Query param:
     *   role (optional) — filter users by role. Case-insensitive.
     *
     * Replaces the old separate /vendors and /drivers endpoints.
     * Same resource (users), different filter — query param is the right tool.
     */
    @GetMapping(ApiConstant.ADMIN_USERS)
    public ResponseEntity<ApiResponse<List<User>>> getUsers(
            @RequestParam(required = false) String role) {

        List<User> users;

        if (role == null || role.isBlank()) {
            users = userRepository.findAll();
            return okResponse("All users fetched successfully!", users);
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!normalizedRole.equals("VENDOR") && !normalizedRole.equals("DRIVER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role filter. Accepted values: VENDOR, DRIVER.");
        }

        users = userRepository.findByRole(normalizedRole);
        return okResponse("Users with role '" + normalizedRole + "' fetched successfully!", users);
    }
}