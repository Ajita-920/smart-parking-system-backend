package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.LogoutRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RefreshTokenRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;
import com.projectwork.Smart.Parking.System.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstant.AUTH_BASE)
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(ApiConstant.AUTH_REGISTER)
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {

        AuthResponseDto response = authService.registerUser(request);
        return okResponse("User registered successfully!", response);
    }

    @PostMapping(ApiConstant.AUTH_LOGIN)
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.loginUser(request);
        return okResponse("Login successful!", response);
    }

    @PostMapping(ApiConstant.AUTH_REFRESH)
    public ResponseEntity<ApiResponse<AuthResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request) {

        AuthResponseDto response = authService.refreshToken(request);
        return okResponse("Token refreshed successfully!", response);
    }

    @PostMapping(ApiConstant.AUTH_LOGOUT)
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody LogoutRequestDto request) {

        String accessToken = authorizationHeader.replaceFirst("(?i)^Bearer\\s+", "").trim();
        authService.logout(accessToken, request);

        return okResponse("Logged out successfully!", null);
    }

    @PostMapping(ApiConstant.AUTH_LOGOUT_ALL)
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication) {
        authService.logoutAll(authentication.getName());
        return okResponse("Logged out from all devices successfully!", null);
    }
}
