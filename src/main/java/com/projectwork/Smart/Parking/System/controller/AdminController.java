package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;
import com.projectwork.Smart.Parking.System.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstant.ADMIN_BASE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController extends BaseController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping(ApiConstant.ADMIN_DASHBOARD)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return okResponse(
                "Admin dashboard fetched successfully!",
                adminService.getDashboard());
    }

    @GetMapping(ApiConstant.ADMIN_BOOKINGS)
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getAllBookings() {
        return okResponse(
                "All bookings fetched successfully!",
                adminService.getAllBookings());
    }

    @GetMapping(ApiConstant.ADMIN_USERS)
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getUsers(
            @RequestParam(required = false) String role) {
        return okResponse(
                "Users fetched successfully!",
                adminService.getUsers(role));
    }
}
