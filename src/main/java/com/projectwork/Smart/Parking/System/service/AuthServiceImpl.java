package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.LogoutRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RefreshTokenRequestDto;
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

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {
        String rawRefreshToken = request.getRefreshToken().trim();
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
                user.getRole());
    }

    @Override
    @Transactional
    public void logout(String accessToken, LogoutRequestDto request) {
        blacklistAccessTokenIfPossible(accessToken);

        String rawRefreshToken = request.getRefreshToken().trim();
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

    private AuthResponseDto buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId());

        String rawRefreshToken = generateRawRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        saveRefreshToken(user, refreshTokenHash);

        return new AuthResponseDto(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtUtil.getAccessTokenExpiresInSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }

    private void saveRefreshToken(User user, String tokenHash) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));

        refreshTokenRepository.save(refreshToken);
    }

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

    private String generateRawRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

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

    private UserRole parseUserRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role. Allowed values: DRIVER, VENDOR.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
