package com.projectwork.Smart.Parking.System.dto.response;

import java.util.UUID;

import lombok.Data;

//new added
@Data
public class UserProfileResponseDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private boolean banned;
    private boolean approved;
}
