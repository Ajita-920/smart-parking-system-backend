package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.parkingLocation.id = :locationId " +
            "AND b.status = 'CONFIRMED' " +
            "AND ((b.startTime < :endTime) AND (b.endTime > :startTime))")
    List<Booking> findOverlappingBookings(Long locationId, LocalDateTime startTime, LocalDateTime endTime);
}