package com.projectwork.Smart.Parking.System.controller;

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
public class AdminController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    // View all bookings
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return ResponseEntity.ok(new ApiResponse("All bookings retrieved", bookings));
    }

    // View all vendors
    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse> getAllVendors() {
        List<User> vendors = userRepository.findByRole("VENDOR");
        return ResponseEntity.ok(new ApiResponse("All vendors retrieved", vendors));
    }

    // View all drivers
    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse> getAllDrivers() {
        List<User> drivers = userRepository.findByRole("DRIVER");
        return ResponseEntity.ok(new ApiResponse("All drivers retrieved", drivers));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboard() {
        long totalBookings = bookingRepository.count();
        long totalVendors = userRepository.countByRole("VENDOR");
        long totalDrivers = userRepository.countByRole("DRIVER");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBookings", totalBookings);
        stats.put("totalVendors", totalVendors);
        stats.put("totalDrivers", totalDrivers);

        return ResponseEntity.ok(new ApiResponse("Admin Dashboard", stats));
    }
}