package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")   // Allow React frontend to connect
public class AuthController {

    @Autowired
    private AuthService authService;

    // ==================== REGISTER ====================
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequestDto request) {

        AuthResponseDto response = authService.registerUser(request);

        return ResponseEntity.ok(new ApiResponse("User registered successfully!", response));
    }

    // ==================== LOGIN ====================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.loginUser(request);

        return ResponseEntity.ok(new ApiResponse("Login successful!", response));
    }
}