package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByStatus(String status);
}
