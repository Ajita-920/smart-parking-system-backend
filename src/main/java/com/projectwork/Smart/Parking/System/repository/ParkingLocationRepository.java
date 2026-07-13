package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingLocationRepository extends JpaRepository<ParkingLocation, UUID> {

    Optional<ParkingLocation> findByIdAndDeletedAtIsNull(UUID id);

    List<ParkingLocation> findByDeletedAtIsNull();

    List<ParkingLocation> findByVendorAndDeletedAtIsNull(User vendor);

    List<ParkingLocation> findByVendor_IdAndDeletedAtIsNull(UUID vendorId);

    boolean existsByNameIgnoreCaseAndVendorAndDeletedAtIsNull(String name, User vendor);

    long countByDeletedAtIsNull();

    long countByVendorAndDeletedAtIsNull(User vendor);

    long countByVendor_IdAndDeletedAtIsNull(UUID vendorId);
}