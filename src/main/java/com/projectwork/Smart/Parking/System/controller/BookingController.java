package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.service.BookingService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@CrossOrigin(origins = "*")
public class BookingController extends BaseController {
 
    @Autowired
    private BookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication) {   // Gets logged-in user

        String userEmail = authentication.getName(); // from JWT

        BookingResponseDto response = bookingService.createBooking(request, userEmail);

        return okResponse("Booking successful!", response);
    }

    @GetMapping("/mybookings")
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(Authentication authentication) {
        String email = authentication.getName();
        List<BookingResponseDto> bookings = bookingService.getMyBookings(email);
        return okResponse("My bookings fetched successfully!", bookings);
    }

    @GetMapping("/debug-user")
    @PreAuthorize("permitAll()")   // ← add this line
    public ResponseEntity<ApiResponse<String>> debugUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return okResponse("No user logged in (anonymous)", null);
        }
        return okResponse(" Logged in user details: ", auth.getName());
    }
}