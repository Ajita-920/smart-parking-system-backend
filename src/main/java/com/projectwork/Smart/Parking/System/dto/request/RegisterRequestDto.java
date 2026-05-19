package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Must be a valid email address.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 6, message = "Password must be at least 6 characters.")
    private String password;

    @NotBlank(message = "Phone number is required.")
    private String phone;

    /**
     * Only DRIVER, VENDOR, or ADMIN are valid roles.
     * Case-insensitive match via regex — the service layer uppercases before saving.
     */
    @NotBlank(message = "Role is required.")
    @Pattern(
        regexp  = "(?i)DRIVER|VENDOR|ADMIN",
        message = "Role must be one of: DRIVER, VENDOR, ADMIN."
    )
    private String role;
}