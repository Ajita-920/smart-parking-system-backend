package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private double amount;

    private String status;           // PENDING, SUCCESS, FAILED

    private String transactionId;

    private String paymentMethod;    // ESEWA or CASH

    private LocalDateTime paidAt;

    private String paymentUrl;       // For eSewa payment link
}