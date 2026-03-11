package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(String vendor);

    long countByRole(String vendor);
}
