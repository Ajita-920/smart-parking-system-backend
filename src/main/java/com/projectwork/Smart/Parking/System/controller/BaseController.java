package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Shared controller helpers for building the project's standard API response
 * shape.
 */
public class BaseController {

    /**
     * Wraps successful controller data in ApiResponse so all endpoints return a
     * consistent JSON structure.
     */
    protected <T> ResponseEntity<ApiResponse<T>> okResponse(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                message,
                data);

        return ResponseEntity.ok(response);
    }
}
