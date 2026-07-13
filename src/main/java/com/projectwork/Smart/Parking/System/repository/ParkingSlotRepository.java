package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, UUID> {

    Optional<ParkingSlot> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT slot FROM ParkingSlot slot
            WHERE slot.id = :id
              AND slot.deletedAt IS NULL
            """)
    Optional<ParkingSlot> findByIdAndDeletedAtIsNullForUpdate(@Param("id") UUID id);

    List<ParkingSlot> findByLocationAndDeletedAtIsNull(ParkingLocation location);

    List<ParkingSlot> findByLocation_IdAndDeletedAtIsNull(UUID locationId);

    List<ParkingSlot> findByLocationAndStatusAndDeletedAtIsNull(
            ParkingLocation location,
            ParkingSlotStatus status);

    List<ParkingSlot> findByLocation_IdAndStatusAndDeletedAtIsNull(
            UUID locationId,
            ParkingSlotStatus status);

    List<ParkingSlot> findByLocationAndVehicleTypeAndStatusAndDeletedAtIsNull(
            ParkingLocation location,
            VehicleType vehicleType,
            ParkingSlotStatus status);

    List<ParkingSlot> findByLocation_IdAndVehicleTypeAndStatusAndDeletedAtIsNull(
            UUID locationId,
            VehicleType vehicleType,
            ParkingSlotStatus status);

    Optional<ParkingSlot> findByLocationAndSlotNumberAndDeletedAtIsNull(
            ParkingLocation location,
            String slotNumber);

    boolean existsByLocationAndSlotNumberAndDeletedAtIsNull(
            ParkingLocation location,
            String slotNumber);

    long countByLocationAndVehicleTypeAndStatusAndDeletedAtIsNull(
            ParkingLocation location,
            VehicleType vehicleType,
            ParkingSlotStatus status);

    long countByLocation_IdAndVehicleTypeAndStatusAndDeletedAtIsNull(
            UUID locationId,
            VehicleType vehicleType,
            ParkingSlotStatus status);

    long countByLocationAndDeletedAtIsNull(ParkingLocation location);

    long countByLocation_IdAndDeletedAtIsNull(UUID locationId);
}
