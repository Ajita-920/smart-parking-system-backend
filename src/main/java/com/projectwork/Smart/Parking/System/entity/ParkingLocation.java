package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;

@Entity
public class ParkingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int totalSlots;
    private int availableSlots;

    @ManyToOne
    @JoinColumn(name="vendor_id")
    private User vendor;

    // getters setters
}
