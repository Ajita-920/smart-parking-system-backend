package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.LoginRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.RegisterRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.AuthResponseDto;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import com.projectwork.Smart.Parking.System.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;   // We will add this in SecurityConfig

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public AuthResponseDto registerUser(RegisterRequestDto request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole().toUpperCase());  // DRIVER / VENDOR / ADMIN

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setUserId(savedUser.getId());
        response.setName(savedUser.getName());
        response.setEmail(savedUser.getEmail());
        response.setRole(savedUser.getRole());

        return response;
    }

    @Override
    public AuthResponseDto loginUser(LoginRequestDto request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }
}