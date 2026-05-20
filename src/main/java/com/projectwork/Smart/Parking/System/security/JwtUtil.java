package com.projectwork.Smart.Parking.System.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    @Value("${app.jwt.secret:yourSuperSecretKeyForSmartParkingSystem2026MakeItLongerInProduction}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    public String generateAccessToken(String email, String role, UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTokenExpirationMs);
        String normalizedRole = role.toUpperCase();
        String roleWithPrefix = "ROLE_" + normalizedRole;

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email)
                .claim("type", ACCESS_TOKEN_TYPE)
                .claim("role", normalizedRole)
                .claim("authorities", List.of(roleWithPrefix))
                .claim("userId", userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Backward-compatible alias. Prefer generateAccessToken(...).
     */
    public String generateToken(String email, String role, UUID userId) {
        return generateAccessToken(email, role, userId);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public Instant extractExpirationInstant(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    public boolean validateToken(String token, String userEmail) {
        Claims claims = extractAllClaims(token);
        String email = claims.getSubject();
        String tokenType = claims.get("type", String.class);

        return email.equals(userEmail)
                && ACCESS_TOKEN_TYPE.equals(tokenType)
                && claims.getExpiration().after(new Date());
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
