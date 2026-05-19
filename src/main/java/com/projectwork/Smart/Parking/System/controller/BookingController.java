package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstant.BOOKING_BASE)
@PreAuthorize("isAuthenticated()")   // all booking routes require a logged-in user
public class BookingController extends BaseController {

    @Autowired
    private BookingService bookingService;

    /**
     * POST /api/bookings
     * Body: BookingRequestDto
     * Creates a new booking for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        BookingResponseDto response = bookingService.createBooking(request, userEmail);
        return okResponse("Booking created successfully!", response);
    }

    /**
     * GET /api/bookings/me
     * Returns all bookings belonging to the currently authenticated user.
     */
    @GetMapping(ApiConstant.BOOKING_ME)
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(
            Authentication authentication) {

        String email = authentication.getName();
        List<BookingResponseDto> bookings = bookingService.getMyBookings(email);
        return okResponse("Bookings fetched successfully!", bookings);
    }

    /**
     * GET /api/bookings/{id}
     * Returns a single booking by ID.
     * Users can only fetch their own; ADMIN can fetch any (enforce in service layer).
     */
    @GetMapping(ApiConstant.BOOKING_BY_ID)
    public ResponseEntity<ApiResponse<BookingResponseDto>> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        BookingResponseDto booking = bookingService.getBookingById(id, email);
        return okResponse("Booking fetched successfully!", booking);
    }
}