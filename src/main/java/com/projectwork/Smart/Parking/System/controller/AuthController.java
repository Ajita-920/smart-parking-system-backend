package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;
import com.projectwork.Smart.Parking.System.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Handles authentication endpoints and owns the HTTP-only refresh-token cookie
 * boundary.
 */
@RestController
@RequestMapping(ApiConstant.AUTH_BASE)
public class AuthController extends BaseController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${app.auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Creates a user account and starts a session by setting the refresh-token
     * cookie.
     */
    @PostMapping(ApiConstant.AUTH_REGISTER)
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {

        AuthResponseDto response = authService.registerUser(request);
        return okResponseWithRefreshCookie("User registered successfully!", response);
    }

    /**
     * Authenticates credentials, returns a short-lived access token, and stores the
     * refresh token in an HTTP-only cookie.
     */
    @PostMapping(ApiConstant.AUTH_LOGIN)
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.loginUser(request);
        return okResponseWithRefreshCookie("Login successful!", response);
    }

    /**
     * Rotates the refresh token from the cookie and returns a new access token.
     */
    @PostMapping(ApiConstant.AUTH_REFRESH)
    public ResponseEntity<ApiResponse<AuthResponseDto>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

        AuthResponseDto response = authService.refreshToken(refreshToken);
        return okResponseWithRefreshCookie("Token refreshed successfully!", response);
    }

    /**
     * Revokes the current refresh token and blacklists the current access token
     * when possible.
     */
    @PostMapping(ApiConstant.AUTH_LOGOUT)
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

        String accessToken = authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "").trim();
        authService.logout(accessToken, refreshToken);

        return okResponseWithClearedRefreshCookie("Logged out successfully!");
    }

    /**
     * Revokes every active refresh token for the authenticated user.
     */
    @PostMapping(ApiConstant.AUTH_LOGOUT_ALL)
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication) {
        authService.logoutAll(authentication.getName());
        return okResponseWithClearedRefreshCookie("Logged out from all devices successfully!");
    }

    /**
     * Builds a successful auth response while keeping the raw refresh token out of
     * the JSON body.
     */
    private ResponseEntity<ApiResponse<AuthResponseDto>> okResponseWithRefreshCookie(
            String message,
            AuthResponseDto data) {

        ApiResponse<AuthResponseDto> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                message,
                data);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(data.getRefreshToken()).toString())
                .body(response);
    }

    /**
     * Clears the browser refresh-token cookie after logout operations.
     */
    private ResponseEntity<ApiResponse<Void>> okResponseWithClearedRefreshCookie(String message) {
        ApiResponse<Void> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                message,
                null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildClearRefreshCookie().toString())
                .body(response);
    }

    /**
     * Creates the HTTP-only cookie that carries the raw refresh token to the
     * browser.
     */
    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(ApiConstant.AUTH_BASE)
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build();
    }

    /**
     * Uses Max-Age=0 with the same cookie path so browsers remove the stored
     * refresh token.
     */
    private ResponseCookie buildClearRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(ApiConstant.AUTH_BASE)
                .maxAge(Duration.ZERO)
                .build();
    }
}
