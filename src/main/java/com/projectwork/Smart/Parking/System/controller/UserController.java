package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.dto.request.UserProfileUpdateRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.UserProfileResponseDto;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(ApiConstant.USER_BASE)
@CrossOrigin(origins = "*")
public class UserController extends BaseController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping(ApiConstant.USER_ME)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getMyProfile(
            org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication.getName());
        return okResponse("User profile fetched successfully!", mapToProfile(user));
    }

    @PutMapping(ApiConstant.USER_PROFILE)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequestDto request,
            org.springframework.security.core.Authentication authentication) {
        User user = getCurrentUser(authentication.getName());

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "currentPassword is required to change password");
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return okResponse("User profile updated successfully!", mapToProfile(user));
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponseDto mapToProfile(User user) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setBanned(user.isBanned());
        dto.setApproved(user.isApproved());
        return dto;
    }
}
