package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLocationRepository extends JpaRepository<ParkingLocation, Long> {

    List<ParkingLocation> findByAvailableSlotsGreaterThan(int slots);

    List<ParkingLocation> findByVendor(User vendor);
}
