package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

        Optional<Booking> findByIdAndDeletedAtIsNull(UUID id);

        Optional<Booking> findByIdAndDriverAndDeletedAtIsNull(UUID id, User driver);

        List<Booking> findByDriverAndDeletedAtIsNull(User driver);

        List<Booking> findByDriver_IdAndDeletedAtIsNull(UUID driverId);

        List<Booking> findByParkingLocationAndDeletedAtIsNull(ParkingLocation parkingLocation);

        List<Booking> findByParkingLocation_IdAndDeletedAtIsNull(UUID locationId);

        List<Booking> findByParkingLocation_Vendor_IdAndDeletedAtIsNull(UUID vendorId);

        List<Booking> findBySlotAndDeletedAtIsNull(ParkingSlot slot);

        List<Booking> findBySlot_IdAndDeletedAtIsNull(UUID slotId);

        Optional<Booking> findFirstBySlotAndStatusAndDeletedAtIsNullOrderByStartTimeDesc(
                        ParkingSlot slot,
                        BookingStatus status);

        Optional<Booking> findFirstBySlot_IdAndStatusAndDeletedAtIsNullOrderByStartTimeDesc(
                        UUID slotId,
                        BookingStatus status);

        List<Booking> findByStatusAndDeletedAtIsNull(BookingStatus status);

        List<Booking> findByDeletedAtIsNull();

        long countByDeletedAtIsNull();

        long countByDriverAndDeletedAtIsNull(User driver);

        long countByDriver_IdAndDeletedAtIsNull(UUID driverId);

        long countByParkingLocationAndDeletedAtIsNull(ParkingLocation parkingLocation);

        long countByParkingLocation_IdAndDeletedAtIsNull(UUID locationId);

        long countByParkingLocation_Vendor_IdAndDeletedAtIsNull(UUID vendorId);

        @Query("""
                        SELECT b FROM Booking b
                        WHERE b.parkingLocation.id = :locationId
                          AND b.status = :status
                          AND b.deletedAt IS NULL
                          AND b.startTime < :endTime
                          AND b.endTime > :startTime
                        """)
        List<Booking> findOverlappingBookings(
                        @Param("locationId") UUID locationId,
                        @Param("status") BookingStatus status,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        default List<Booking> findOverlappingConfirmedBookings(
                        UUID locationId,
                        LocalDateTime startTime,
                        LocalDateTime endTime) {
                return findOverlappingBookings(
                                locationId,
                                BookingStatus.CONFIRMED,
                                startTime,
                                endTime);
        }
}
