package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingCancelResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstant.BOOKING_BASE)
@PreAuthorize("isAuthenticated()")
public class BookingController extends BaseController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BookingResponseDto>> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication) {
        BookingResponseDto response = bookingService.createBooking(request, authentication.getName());
        return okResponse("Booking created successfully!", response);
    }

    @GetMapping(ApiConstant.BOOKING_ME)
    public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getMyBookings(
            Authentication authentication) {
        List<BookingResponseDto> bookings = bookingService.getMyBookings(authentication.getName());
        return okResponse("Bookings fetched successfully!", bookings);
    }

    @PutMapping(ApiConstant.BOOKING_CANCEL)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BookingCancelResponseDto>> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        BookingCancelResponseDto response = bookingService.cancelBooking(bookingId, authentication.getName());
        return okResponse("Booking cancellation processed!", response);
    }

    @GetMapping(ApiConstant.BOOKING_BY_ID)
    public ResponseEntity<ApiResponse<BookingResponseDto>> getBookingById(
            @PathVariable UUID id,
            Authentication authentication) {
        BookingResponseDto booking = bookingService.getBookingById(id, authentication.getName());
        return okResponse("Booking fetched successfully!", booking);
    }

    @GetMapping(ApiConstant.BOOKING_DEBUG_USER_LEGACY)
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<String>> debugUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return okResponse("No user logged in (anonymous)", null);
        }

        return okResponse("Logged in user details:", authentication.getName());
    }
}