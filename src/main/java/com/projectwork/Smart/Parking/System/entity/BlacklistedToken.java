package com.projectwork.Smart.Parking.System.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "blacklisted_tokens", indexes = {
        @Index(name = "idx_blacklisted_tokens_jti", columnList = "jti"),
        @Index(name = "idx_blacklisted_tokens_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken extends BaseEntity {

    @NotBlank
    @Column(name = "jti", nullable = false, unique = true, length = 100)
    private String jti;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}