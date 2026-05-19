package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.UUID;

import com.projectwork.Smart.Parking.System.entity.UserRole;

import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String type = "Bearer";
    private UUID userId;
    private String name;
    private String email;
    private UserRole role;
}