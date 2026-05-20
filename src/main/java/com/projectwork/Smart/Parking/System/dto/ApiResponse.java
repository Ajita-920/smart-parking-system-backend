package com.projectwork.Smart.Parking.System.dto;

import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString
public class ApiResponse<T> {

    private int responseCode;
    private String responseMessage;
    private Instant timestamp;
    private T data;

    public ApiResponse(int responseCode, String responseMessage, T data) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.timestamp = Instant.now();
        this.data = data;
    }

    public ApiResponse(int responseCode, String responseMessage, Instant timestamp, T data) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.timestamp = timestamp;
        this.data = data;
    }
}