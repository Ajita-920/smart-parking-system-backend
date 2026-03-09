package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String type = "Bearer";

    private Long userId;
    private String name;
    private String email;
    private String role;
}