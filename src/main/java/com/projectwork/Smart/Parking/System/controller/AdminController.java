package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;
import com.projectwork.Smart.Parking.System.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints for platform-wide users, bookings, and dashboard
 * actions.
 */
@RestController
@RequestMapping(ApiConstant.ADMIN_BASE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends BaseController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Returns high-level platform counts for the admin dashboard.
     */
    @GetMapping(ApiConstant.ADMIN_DASHBOARD)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return okResponse(
                "Admin dashboard fetched successfully!",
                adminService.getDashboard());
    }

    /**
     * Lists all active bookings in the system.
     */
    @GetMapping(ApiConstant.ADMIN_BOOKINGS)
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getAllBookings() {
        return okResponse(
                "All bookings fetched successfully!",
                adminService.getAllBookings());
    }

    /**
     * Lists active users, optionally filtered by role.
     */
    @GetMapping(ApiConstant.ADMIN_USERS)
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getUsers(
            @RequestParam(required = false) String role) {
        return okResponse(
                "Users fetched successfully!",
                adminService.getUsers(role));
    }

    /**
     * Bans a non-admin user from authenticating.
     */
    @PutMapping(ApiConstant.ADMIN_USER_BAN)
    public ResponseEntity<ApiResponse<UserResponseDto>> banUser(@PathVariable UUID id) {
        return okResponse(
                "User banned successfully!",
                adminService.banUser(id));
    }

    /**
     * Soft-deletes a non-admin user and related vendor parking locations when
     * applicable.
     */
    @DeleteMapping(ApiConstant.ADMIN_USER_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return okResponse("User deleted successfully!", null);
    }

    /**
     * Marks a vendor account as approved so it can authenticate.
     */
    @PutMapping(ApiConstant.ADMIN_VENDOR_APPROVE)
    public ResponseEntity<ApiResponse<UserResponseDto>> approveVendor(@PathVariable UUID id) {
        return okResponse(
                "Vendor approved successfully!",
                adminService.approveVendor(id));
    }

    /**
     * Soft-deletes a vendor and all active parking locations owned by that vendor.
     */
    @DeleteMapping(ApiConstant.ADMIN_VENDOR_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteVendor(@PathVariable UUID id) {
        adminService.deleteVendor(id);
        return okResponse("Vendor deleted successfully!", null);
    }
}
