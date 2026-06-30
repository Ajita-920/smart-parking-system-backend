package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.VendorBookingStatusRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.WalkInBookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;
import com.projectwork.Smart.Parking.System.service.BookingService;
import com.projectwork.Smart.Parking.System.service.VendorDashboardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Vendor-only endpoints for dashboard data and managing bookings at vendor
 * parking locations.
 */
@RestController
@RequestMapping(ApiConstant.VENDOR_BASE)
@PreAuthorize("hasRole('VENDOR')")
public class VendorController extends BaseController {

        private final VendorDashboardService vendorDashboardService;
        private final BookingService bookingService;

        public VendorController(
                        VendorDashboardService vendorDashboardService,
                        BookingService bookingService) {
                this.vendorDashboardService = vendorDashboardService;
                this.bookingService = bookingService;
        }

        /**
         * Returns booking and parking-location metrics for the authenticated vendor.
         */
        @GetMapping(ApiConstant.VENDOR_DASHBOARD)
        public ResponseEntity<ApiResponse<VendorDashboardResponseDto>> getDashboard(
                        Authentication authentication) {
                return okResponse(
                                "Vendor dashboard fetched successfully!",
                                vendorDashboardService.getDashboard(authentication.getName()));
        }

        /**
         * Returns bookings for parking locations owned by the authenticated vendor.
         */
        @GetMapping(ApiConstant.VENDOR_BOOKINGS)
        public ResponseEntity<ApiResponse<List<BookingResponseDto>>> getBookings(
                        @RequestParam(required = false) UUID locationId,
                        Authentication authentication) {
                return okResponse(
                                "Vendor bookings fetched successfully!",
                                bookingService.getVendorBookings(authentication.getName(), locationId));
        }

        /**
         * Creates an immediate walk-in booking for an available slot at a vendor-owned
         * parking location.
         */
        @PostMapping(ApiConstant.VENDOR_WALK_IN_BOOKING)
        public ResponseEntity<ApiResponse<BookingResponseDto>> createWalkInBooking(
                        @Valid @RequestBody WalkInBookingRequestDto request,
                        Authentication authentication) {
                return okResponse(
                                "Walk-in booking created successfully!",
                                bookingService.createWalkInBooking(request, authentication.getName()));
        }

        /**
         * Allows a vendor to update the status of a booking for their own parking
         * location.
         */
        @PutMapping(ApiConstant.VENDOR_BOOKING_STATUS)
        public ResponseEntity<ApiResponse<BookingResponseDto>> updateBookingStatus(
                        @PathVariable UUID bookingId,
                        @Valid @RequestBody VendorBookingStatusRequestDto request,
                        Authentication authentication) {
                return okResponse(
                                "Booking status updated successfully!",
                                bookingService.updateVendorBookingStatus(
                                                bookingId,
                                                request,
                                                authentication.getName()));
        }
}
