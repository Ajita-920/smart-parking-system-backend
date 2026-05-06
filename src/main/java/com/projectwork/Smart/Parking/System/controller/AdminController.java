package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminController extends BaseController{


    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    // View all bookings
    @GetMapping(ApiConstant.BOOKINGS)
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return okResponse("All bookings retrieved", bookings);
    }

    // View all vendors
    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<List<User>>> getAllVendors() {
        List<User> vendors = userRepository.findByRole("VENDOR");
        return okResponse("All vendors retrieved", vendors);
    }

    // View all drivers
    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<User>>> getAllDrivers() {
        List<User> drivers = userRepository.findByRole("DRIVER");
        return okResponse("All drivers retrieved", drivers);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        long totalBookings = bookingRepository.count();
        long totalVendors = userRepository.countByRole("VENDOR");
        long totalDrivers = userRepository.countByRole("DRIVER");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("totalVendors", totalVendors);
        stats.put("totalDrivers", totalDrivers);

        return okResponse("Admin Dashboard", stats);
    }
}