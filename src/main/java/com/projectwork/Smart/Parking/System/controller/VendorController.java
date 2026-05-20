package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;
import com.projectwork.Smart.Parking.System.service.VendorDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstant.VENDOR_BASE)
@PreAuthorize("hasRole('VENDOR')")
public class VendorController extends BaseController {

        private final VendorDashboardService vendorDashboardService;

        public VendorController(VendorDashboardService vendorDashboardService) {
                this.vendorDashboardService = vendorDashboardService;
        }

        @GetMapping(ApiConstant.VENDOR_DASHBOARD)
        public ResponseEntity<ApiResponse<VendorDashboardResponseDto>> getDashboard(
                        Authentication authentication) {
                return okResponse(
                                "Vendor dashboard fetched successfully!",
                                vendorDashboardService.getDashboard(authentication.getName()));
        }
}
