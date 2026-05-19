package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> okResponse(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                message,
                LocalDateTime.now(),
                data
        );
        return ResponseEntity.ok(response);
    }
}