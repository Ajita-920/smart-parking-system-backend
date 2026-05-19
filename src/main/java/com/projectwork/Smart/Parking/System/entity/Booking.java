package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
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


    @FutureOrPresent
    private LocalDateTime startTime;

    @Future
    private LocalDateTime endTime;
    private String status;
    //booking cancel feature added
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    private LocalDateTime cancelledAt;
    private Double refundAmount;


    @ManyToOne
    @JoinColumn(name = "location_id")
    private ParkingLocation parkingLocation;

    private Double totalAmount;

}
