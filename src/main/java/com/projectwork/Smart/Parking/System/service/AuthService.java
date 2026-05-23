package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;

/**
 * Authentication use cases for registration, login, refresh-token rotation, and
 * logout.
 */
public interface AuthService {

    /**
     * Registers a new user and returns session data for the new account.
     */
    AuthResponseDto registerUser(RegisterRequestDto request);

    /**
     * Verifies credentials and returns a new access/refresh token pair.
     */
    AuthResponseDto loginUser(LoginRequestDto request);

    /**
     * Validates and rotates the raw refresh token received from the HTTP-only
     * cookie.
     */
    AuthResponseDto refreshToken(String rawRefreshToken);

    /**
     * Revokes the current refresh token and blacklists the access token when it is
     * still valid.
     */
    void logout(String accessToken, String rawRefreshToken);

    /**
     * Revokes every refresh token for the given user email.
     */
    void logoutAll(String currentUserEmail);
}
