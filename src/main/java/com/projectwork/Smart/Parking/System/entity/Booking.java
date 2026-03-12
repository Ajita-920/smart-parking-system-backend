package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private ParkingSlot slot;

    @ManyToOne
    private User driver;


    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;


    @ManyToOne
    @JoinColumn(name = "location_id")
    private ParkingLocation parkingLocation;

    private Double totalAmount;

}