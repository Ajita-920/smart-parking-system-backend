package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {


    Optional<Payment> findByTransactionId(String oid);
}

