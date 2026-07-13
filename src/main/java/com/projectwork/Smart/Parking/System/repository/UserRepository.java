package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndDeletedAtIsNull(UserRole role);

    List<User> findByDeletedAtIsNull();

    long countByDeletedAtIsNull();

    long countByRole(UserRole role);

    long countByRoleAndDeletedAtIsNull(UserRole role);
}
