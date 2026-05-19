package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(ApiConstant.VENDOR_BASE)
@PreAuthorize("hasRole('VENDOR')")
public class VendorController extends BaseController {

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/vendors/dashboard
     * Returns aggregated slot stats across all of the vendor's parking locations.
     */
    @GetMapping(ApiConstant.VENDOR_DASHBOARD)
    public ResponseEntity<ApiResponse<VendorDashboardResponseDto>> getDashboard(
            Authentication authentication) {

        String email = authentication.getName();
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found."));

        List<ParkingLocation> parkings = parkingLocationRepository.findByVendor(vendor);

        VendorDashboardResponseDto dashboard = new VendorDashboardResponseDto();
        dashboard.setTotalParkingLocations(parkings.size());

        List<VendorDashboardResponseDto.LocationSlotSummary> summaries = new ArrayList<>();

        for (ParkingLocation parking : parkings) {
            SlotBreakdown split = splitSlots(parking.getTotalSlots(), parking.getAvailableSlots());

            VendorDashboardResponseDto.LocationSlotSummary summary =
                    new VendorDashboardResponseDto.LocationSlotSummary();
            summary.setId(parking.getId());
            summary.setName(parking.getName());
            summary.setTotalSlots(split.totalSlots());
            summary.setAvailableSlots(split.availableSlots());
            summary.setOccupiedSlots(split.occupiedSlots());

            summary.getTwoWheelerSlots().setTotal(split.twoWheelerTotalSlots());
            summary.getTwoWheelerSlots().setAvailable(split.twoWheelerAvailableSlots());
            summary.getTwoWheelerSlots().setOccupied(split.twoWheelerOccupiedSlots());

            summary.getFourWheelerSlots().setTotal(split.fourWheelerTotalSlots());
            summary.getFourWheelerSlots().setAvailable(split.fourWheelerAvailableSlots());
            summary.getFourWheelerSlots().setOccupied(split.fourWheelerOccupiedSlots());

            summaries.add(summary);

            // Accumulate totals into dashboard
            dashboard.setTotalSlots(dashboard.getTotalSlots() + split.totalSlots());
            dashboard.setAvailableSlots(dashboard.getAvailableSlots() + split.availableSlots());
            dashboard.setOccupiedSlots(dashboard.getOccupiedSlots() + split.occupiedSlots());

            dashboard.getTwoWheelerSlots().setTotal(
                    dashboard.getTwoWheelerSlots().getTotal() + split.twoWheelerTotalSlots());
            dashboard.getTwoWheelerSlots().setAvailable(
                    dashboard.getTwoWheelerSlots().getAvailable() + split.twoWheelerAvailableSlots());
            dashboard.getTwoWheelerSlots().setOccupied(
                    dashboard.getTwoWheelerSlots().getOccupied() + split.twoWheelerOccupiedSlots());

            dashboard.getFourWheelerSlots().setTotal(
                    dashboard.getFourWheelerSlots().getTotal() + split.fourWheelerTotalSlots());
            dashboard.getFourWheelerSlots().setAvailable(
                    dashboard.getFourWheelerSlots().getAvailable() + split.fourWheelerAvailableSlots());
            dashboard.getFourWheelerSlots().setOccupied(
                    dashboard.getFourWheelerSlots().getOccupied() + split.fourWheelerOccupiedSlots());
        }

        dashboard.setLocations(summaries);
        return okResponse("Vendor dashboard fetched successfully!", dashboard);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private SlotBreakdown splitSlots(int totalSlots, int availableSlots) {
        int safeTotalSlots     = Math.max(totalSlots, 0);
        int safeAvailableSlots = Math.min(Math.max(availableSlots, 0), safeTotalSlots);

        int fourWheelerTotal     = safeTotalSlots / 2;
        int twoWheelerTotal      = safeTotalSlots - fourWheelerTotal;
        int fourWheelerAvailable = safeTotalSlots == 0 ? 0
                : (int) Math.floor((double) safeAvailableSlots * fourWheelerTotal / safeTotalSlots);
        int twoWheelerAvailable  = safeAvailableSlots - fourWheelerAvailable;

        return new SlotBreakdown(
                safeTotalSlots,
                safeAvailableSlots,
                safeTotalSlots - safeAvailableSlots,
                twoWheelerTotal,
                twoWheelerAvailable,
                twoWheelerTotal - twoWheelerAvailable,
                fourWheelerTotal,
                fourWheelerAvailable,
                fourWheelerTotal - fourWheelerAvailable
        );
    }

    private record SlotBreakdown(
            int totalSlots,
            int availableSlots,
            int occupiedSlots,
            int twoWheelerTotalSlots,
            int twoWheelerAvailableSlots,
            int twoWheelerOccupiedSlots,
            int fourWheelerTotalSlots,
            int fourWheelerAvailableSlots,
            int fourWheelerOccupiedSlots) {}
}