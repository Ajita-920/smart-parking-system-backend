package com.projectwork.Smart.Parking.System.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private ParkingSlot slot;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    public void setUser(User user) {
    }

    public void setSlot(ParkingSlot slot) {
    }

    public void setStatus(String booked) {
    }
}