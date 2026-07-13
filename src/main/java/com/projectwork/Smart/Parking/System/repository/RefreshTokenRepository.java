package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.RefreshToken;
import com.projectwork.Smart.Parking.System.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndDeletedAtIsNull(String tokenHash);

    List<RefreshToken> findByUserAndDeletedAtIsNull(User user);

    long deleteByExpiresAtBefore(Instant now);
}