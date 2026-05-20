package com.projectwork.Smart.Parking.System.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDto {

    @NotBlank(message = "Refresh token is required.")
    private String refreshToken;
}