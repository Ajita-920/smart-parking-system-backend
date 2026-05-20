package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.LogoutRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RefreshTokenRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;

public interface AuthService {

    AuthResponseDto registerUser(RegisterRequestDto request);

    AuthResponseDto loginUser(LoginRequestDto request);

    AuthResponseDto refreshToken(RefreshTokenRequestDto request);

    void logout(String accessToken, LogoutRequestDto request);

    void logoutAll(String currentUserEmail);
}