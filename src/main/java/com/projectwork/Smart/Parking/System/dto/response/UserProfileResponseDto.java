package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

@Data
public class UserProfileResponseDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
}
