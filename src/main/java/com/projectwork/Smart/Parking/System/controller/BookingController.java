package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication) {   // Gets logged-in user

        String userEmail = authentication.getName(); // from JWT

        BookingResponseDto response = bookingService.createBooking(request, userEmail);

        return ResponseEntity.ok(new ApiResponse("Booking successful!", response));
    }
}