package com.projectwork.Smart.Parking.System.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApiResponse(String message, Object data) {
        this.success = true;
        this.message = message;
        this.data = data;
    }
}
