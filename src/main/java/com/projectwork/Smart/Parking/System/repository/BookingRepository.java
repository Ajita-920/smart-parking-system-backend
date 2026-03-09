package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);
}
