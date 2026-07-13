package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;
import com.projectwork.Smart.Parking.System.entity.BlacklistedToken;
import com.projectwork.Smart.Parking.System.entity.RefreshToken;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.repository.BlacklistedTokenRepository;
import com.projectwork.Smart.Parking.System.repository.RefreshTokenRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Implements stateless authentication using short-lived JWT access tokens and
 * rotating refresh tokens stored as hashes.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            BlacklistedTokenRepository blacklistedTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthResponseDto registerUser(RegisterRequestDto request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }

        UserRole role = parseUserRole(request.getRole());

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setRole(role);
        user.setApproved(role != UserRole.VENDOR);

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponseDto loginUser(LoginRequestDto request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        validateUserCanAuthenticate(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token is required.");
        }

        rawRefreshToken = rawRefreshToken.trim();
        String oldTokenHash = hashToken(rawRefreshToken);

        RefreshToken oldRefreshToken = refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(oldTokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token."));

        if (oldRefreshToken.isRevoked()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token has been revoked.");
        }

        if (oldRefreshToken.isExpired()) {
            oldRefreshToken.revoke();
            refreshTokenRepository.save(oldRefreshToken);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token has expired.");
        }

        User user = oldRefreshToken.getUser();

        if (user == null || user.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is no longer active.");
        }

        validateUserCanAuthenticate(user);

        /*
         * Refresh tokens are single-use. After a successful refresh, the old token is
         * revoked and linked to the replacement token hash for auditability.
         */
        String newRawRefreshToken = generateRawRefreshToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        oldRefreshToken.revoke();
        oldRefreshToken.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(oldRefreshToken);

        saveRefreshToken(user, newTokenHash);

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId());

        return new AuthResponseDto(
                newAccessToken,
                newRawRefreshToken,
                "Bearer",
                jwtUtil.getAccessTokenExpiresInSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isBanned(),
                user.isApproved());
    }

    @Override
    @Transactional
    public void logout(String accessToken, String rawRefreshToken) {
        blacklistAccessTokenIfPossible(accessToken);

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        rawRefreshToken = rawRefreshToken.trim();
        String tokenHash = hashToken(rawRefreshToken);

        refreshTokenRepository.findByTokenHashAndDeletedAtIsNull(tokenHash)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Override
    @Transactional
    public void logoutAll(String currentUserEmail) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."));

        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserAndDeletedAtIsNull(user);

        for (RefreshToken refreshToken : refreshTokens) {
            if (!refreshToken.isRevoked()) {
                refreshToken.revoke();
            }
        }

        refreshTokenRepository.saveAll(refreshTokens);
    }

    /**
     * Creates the access token plus raw refresh token for a successful auth event.
     */
    private AuthResponseDto buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId());

        String rawRefreshToken = generateRawRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        /*
         * Only the hash is stored in the database. The raw token is returned to the
         * controller so it can be placed in an HTTP-only cookie.
         */
        saveRefreshToken(user, refreshTokenHash);

        return new AuthResponseDto(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtUtil.getAccessTokenExpiresInSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isBanned(),
                user.isApproved());
    }

    /**
     * Applies account-state rules that block login/refresh even when credentials or
     * tokens are otherwise valid.
     */
    private void validateUserCanAuthenticate(User user) {
        if (user.isBanned()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account has been banned.");
        }

        if (user.getRole() == UserRole.VENDOR && !user.isApproved()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vendor account is not approved yet.");
        }
    }

    /**
     * Persists a refresh token hash with its expiry.
     */
    private void saveRefreshToken(User user, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Stores the access-token JTI until expiry so a logged-out token cannot be used
     * again.
     */
    private void blacklistAccessTokenIfPossible(String accessToken) {
        try {
            String jti = jwtUtil.extractJti(accessToken);
            Instant expiresAt = jwtUtil.extractExpirationInstant(accessToken);

            if (jti == null || expiresAt == null || Instant.now().isAfter(expiresAt)) {
                return;
            }

            boolean alreadyBlacklisted = blacklistedTokenRepository
                    .existsByJtiAndExpiresAtAfterAndDeletedAtIsNull(jti, Instant.now());

            if (alreadyBlacklisted) {
                return;
            }

            BlacklistedToken blacklistedToken = new BlacklistedToken();
            blacklistedToken.setJti(jti);
            blacklistedToken.setExpiresAt(expiresAt);

            blacklistedTokenRepository.save(blacklistedToken);
        } catch (Exception ignored) {
            /*
             * Logout should still continue when the access token is expired/malformed.
             * The refresh token revocation is the more important logout step.
             */
        }
    }

    /**
     * Generates a high-entropy opaque refresh token for the browser cookie.
     */
    private String generateRawRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * Hashes a raw refresh token before lookup/storage so leaked database rows do
     * not expose usable refresh tokens.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash token.", ex);
        }
    }

    /**
     * Converts request role text into a supported UserRole.
     */
    private UserRole parseUserRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role. Allowed values: DRIVER, VENDOR.");
        }
    }

    /**
     * Normalizes emails before lookup/storage to avoid duplicate accounts by case.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
