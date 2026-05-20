package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    private UUID userId;
    private String name;
    private String email;
    private UserRole role;
    private boolean banned;
    private boolean approved;
}
