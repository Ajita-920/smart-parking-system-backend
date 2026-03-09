package com.projectwork.Smart.Parking.System.entity;


import jakarta.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String role; // DRIVER, VENDOR, ADMIN

    private String phone;

    // getters and setters
}
