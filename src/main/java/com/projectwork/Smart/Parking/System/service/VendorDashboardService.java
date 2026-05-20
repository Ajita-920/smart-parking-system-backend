package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;

public interface VendorDashboardService {

    VendorDashboardResponseDto getDashboard(String currentUserEmail);
}
