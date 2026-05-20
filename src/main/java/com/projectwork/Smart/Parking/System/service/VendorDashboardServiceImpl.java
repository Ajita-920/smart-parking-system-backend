package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.VendorDashboardResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VendorDashboardServiceImpl implements VendorDashboardService {

    private final ParkingLocationRepository parkingLocationRepository;
    private final UserRepository userRepository;

    public VendorDashboardServiceImpl(
            ParkingLocationRepository parkingLocationRepository,
            UserRepository userRepository
    ) {
        this.parkingLocationRepository = parkingLocationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public VendorDashboardResponseDto getDashboard(String currentUserEmail) {
        User vendor = userRepository.findByEmailAndDeletedAtIsNull(currentUserEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vendor not found."
                ));

        List<ParkingLocation> locations = parkingLocationRepository.findByVendorAndDeletedAtIsNull(vendor);

        VendorDashboardResponseDto dashboard = new VendorDashboardResponseDto();
        dashboard.setTotalParkingLocations(locations.size());

        for (ParkingLocation location : locations) {
            addLocationSummary(dashboard, location);
        }

        return dashboard;
    }

    private void addLocationSummary(
            VendorDashboardResponseDto dashboard,
            ParkingLocation location
    ) {
        int totalSlots = location.getTotalSlots();
        int availableSlots = location.getAvailableSlots();
        int occupiedSlots = totalSlots - availableSlots;

        int totalTwoWheelerSlots = location.getTotalTwoWheelerSlots();
        int availableTwoWheelerSlots = location.getAvailableTwoWheelerSlots();
        int occupiedTwoWheelerSlots = totalTwoWheelerSlots - availableTwoWheelerSlots;

        int totalFourWheelerSlots = location.getTotalFourWheelerSlots();
        int availableFourWheelerSlots = location.getAvailableFourWheelerSlots();
        int occupiedFourWheelerSlots = totalFourWheelerSlots - availableFourWheelerSlots;

        VendorDashboardResponseDto.LocationSlotSummary summary =
                new VendorDashboardResponseDto.LocationSlotSummary();

        summary.setId(location.getId());
        summary.setName(location.getName());
        summary.setTotalSlots(totalSlots);
        summary.setAvailableSlots(availableSlots);
        summary.setOccupiedSlots(occupiedSlots);

        summary.getTwoWheelerSlots().setTotal(totalTwoWheelerSlots);
        summary.getTwoWheelerSlots().setAvailable(availableTwoWheelerSlots);
        summary.getTwoWheelerSlots().setOccupied(occupiedTwoWheelerSlots);

        summary.getFourWheelerSlots().setTotal(totalFourWheelerSlots);
        summary.getFourWheelerSlots().setAvailable(availableFourWheelerSlots);
        summary.getFourWheelerSlots().setOccupied(occupiedFourWheelerSlots);

        dashboard.getLocations().add(summary);

        dashboard.setTotalSlots(dashboard.getTotalSlots() + totalSlots);
        dashboard.setAvailableSlots(dashboard.getAvailableSlots() + availableSlots);
        dashboard.setOccupiedSlots(dashboard.getOccupiedSlots() + occupiedSlots);

        dashboard.getTwoWheelerSlots().setTotal(
                dashboard.getTwoWheelerSlots().getTotal() + totalTwoWheelerSlots
        );
        dashboard.getTwoWheelerSlots().setAvailable(
                dashboard.getTwoWheelerSlots().getAvailable() + availableTwoWheelerSlots
        );
        dashboard.getTwoWheelerSlots().setOccupied(
                dashboard.getTwoWheelerSlots().getOccupied() + occupiedTwoWheelerSlots
        );

        dashboard.getFourWheelerSlots().setTotal(
                dashboard.getFourWheelerSlots().getTotal() + totalFourWheelerSlots
        );
        dashboard.getFourWheelerSlots().setAvailable(
                dashboard.getFourWheelerSlots().getAvailable() + availableFourWheelerSlots
        );
        dashboard.getFourWheelerSlots().setOccupied(
                dashboard.getFourWheelerSlots().getOccupied() + occupiedFourWheelerSlots
        );
    }
}
