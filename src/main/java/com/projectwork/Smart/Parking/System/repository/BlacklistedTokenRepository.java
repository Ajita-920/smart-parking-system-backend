package com.projectwork.Smart.Parking.System.repository;

import com.projectwork.Smart.Parking.System.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    boolean existsByJtiAndExpiresAtAfterAndDeletedAtIsNull(String jti, Instant now);

    long deleteByExpiresAtBefore(Instant now);
}