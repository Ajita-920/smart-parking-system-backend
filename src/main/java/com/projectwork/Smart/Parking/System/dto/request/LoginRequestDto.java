package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}