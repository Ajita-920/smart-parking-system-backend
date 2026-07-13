package com.projectwork.Smart.Parking.System.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String TEST_SECRET =
            "testSecretForJwtUtilityClaimsValidation2026MustBeLongEnough";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", 900_000L);
    }

    @Test
    void generateAccessToken_shouldValidateAndContainExpectedClaims() {
        UUID userId = UUID.randomUUID();

        String token = jwtUtil.generateAccessToken("driver@example.com", "DRIVER", userId);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token, "driver@example.com"));
        assertEquals("driver@example.com", jwtUtil.extractUsername(token));

        Claims claims = parseClaims(token);
        assertEquals("driver@example.com", claims.getSubject());
        assertEquals("ACCESS", claims.get("type", String.class));
        assertEquals("DRIVER", claims.get("role", String.class));
        assertEquals(userId.toString(), claims.get("userId", String.class));
        assertNotNull(claims.getId());
    }

    @Test
    void validateToken_shouldRejectMalformedToken() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("not-a-jwt", "driver@example.com"));
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
