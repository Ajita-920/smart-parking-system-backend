package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank
    private String name;

    @Email
    private String email;

    private String password;
    private String phone;
    @NotNull
    private String role;
}