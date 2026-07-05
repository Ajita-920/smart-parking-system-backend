package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.UserRole;
import lombok.Data;

import java.util.UUID;
import java.time.Instant;

@Data
public class UserResponseDto {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private boolean banned;
    private boolean approved;
    private Instant createdAt;
}
