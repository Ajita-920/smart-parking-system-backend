package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLocationRepository extends JpaRepository<ParkingLocation, Long> {

    List<ParkingLocation> findByAvailableSlotsGreaterThan(int slots);
}
