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
public class AuthController extends BaseController {

    @Autowired
    private AuthService authService;


//    @GetMapping("/hi")
//    public ResponseEntity<ApiResponse<String>> hello(){
//       String res= "Hello";
//        return okResponse("hiiiii", res);
//
//    }
    // register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response =authService.registerUser(request);
        return okResponse("User registered successfully!", response);
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto response = authService.loginUser(request);

        return okResponse("Login successful!", response);
    }
}