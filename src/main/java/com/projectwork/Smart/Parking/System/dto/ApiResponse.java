package com.projectwork.Smart.Parking.System.dto;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class ApiResponse<T> {
    private int responseCode;
    private String responseMessage;
    private LocalDateTime timestamp;
    private T data;

    public ApiResponse(int responseCode, String responseMessage, LocalDateTime timestamp, T data) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.timestamp = timestamp;
        this.data = data;
    }
}
