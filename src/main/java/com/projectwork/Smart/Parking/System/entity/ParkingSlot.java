package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;

@Entity
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slotNumber;

    private String status; // AVAILABLE / OCCUPIED

    @ManyToOne
    @JoinColumn(name="location_id")
    private ParkingLocation location;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public ParkingLocation getLocation() {
        return location;
    }

    public void setLocation(ParkingLocation location) {
        this.location = location;
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(String occupied) {
    }
}