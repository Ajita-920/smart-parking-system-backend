package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Name is required.")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Must be a valid email address.")
    @Size(max = 150, message = "Email must not exceed 150 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String password;

    @NotBlank(message = "Phone number is required.")
    @Pattern(
            regexp = "^[0-9+\\-\\s()]{7,20}$",
            message = "Phone number must be valid."
    )
    private String phone;

    @NotBlank(message = "Role is required.")
    @Pattern(
            regexp = "^(?i)(DRIVER|VENDOR)$",
            message = "Role must be one of: DRIVER, VENDOR."
    )
    private String role;
}