package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "parking_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    @JoinColumn(name = "vendor_id")
    private User vendor;
}