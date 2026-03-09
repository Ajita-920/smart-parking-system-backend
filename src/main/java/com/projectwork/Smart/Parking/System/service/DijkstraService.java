package com.projectwork.Smart.Parking.System.service.algorithm;

import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DijkstraService {

    @Autowired
    private ParkingLocationRepository parkingLocationRepository;

    public ParkingLocationResponseDto findNearestParking(double userLat, double userLon) {
        List<ParkingLocation> locations = parkingLocationRepository.findByAvailableSlotsGreaterThan(0);

        if (locations.isEmpty()) {
            throw new RuntimeException("No parking available nearby!");
        }

        ParkingLocation nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ParkingLocation loc : locations) {
            double dist = haversine(userLat, userLon, loc.getLatitude(), loc.getLongitude());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = loc;
            }
        }

        ParkingLocationResponseDto dto = new ParkingLocationResponseDto();
        dto.setId(nearest.getId());
        dto.setName(nearest.getName());
        dto.setAddress(nearest.getAddress());
        dto.setLatitude(nearest.getLatitude());
        dto.setLongitude(nearest.getLongitude());
        dto.setAvailableSlots(nearest.getAvailableSlots());
        dto.setDistance(minDistance);
        dto.setVendorName(nearest.getVendor().getName());

        return dto;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}