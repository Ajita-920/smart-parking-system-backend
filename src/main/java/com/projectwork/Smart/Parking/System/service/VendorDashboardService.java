package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;

/**
 * Vendor dashboard summary contract.
 */
public interface VendorDashboardService {

    /**
     * Builds dashboard metrics for the authenticated vendor.
     */
    VendorDashboardResponseDto getDashboard(String currentUserEmail);
}
