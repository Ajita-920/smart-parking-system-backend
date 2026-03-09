package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

@Data
public class AuthResponseDto {
    private Long userId;
    private String email;
    private String password;
    private String role;


}
