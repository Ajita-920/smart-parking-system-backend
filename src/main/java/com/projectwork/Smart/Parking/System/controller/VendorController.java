package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.VendorBookingStatusRequestDto;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

        @GetMapping(ApiConstant.VENDOR_DASHBOARD)
        public ResponseEntity<ApiResponse<VendorDashboardResponseDto>> getDashboard(
                        Authentication authentication) {
                return okResponse(
                                "Vendor dashboard fetched successfully!",
                                vendorDashboardService.getDashboard(authentication.getName()));
        }

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
