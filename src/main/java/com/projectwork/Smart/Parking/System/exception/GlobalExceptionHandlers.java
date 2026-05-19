package com.projectwork.Smart.Parking.System.exception;

import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandlers {

    /**
     * Handles @Valid / @Validated failures on request bodies.
     * Returns 400 with a field → message map inside the standard ApiResponse envelope.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /**
     * Handles missing required @RequestParam values.
     * e.g. GET /api/parking/nearby without ?lat= or ?lng=
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(
            MissingServletRequestParameterException ex) {

        String message = "Required parameter '" + ex.getParameterName() + "' is missing.";
        return buildResponse(HttpStatus.BAD_REQUEST, message, null);
    }

    /**
     * Handles ResponseStatusException — thrown explicitly in controllers and services
     * with a specific HTTP status (404, 403, 400, 409, etc.).
     * Without this handler every ResponseStatusException fell through to the 500 catch-all.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(
            ResponseStatusException ex) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildResponse(status, ex.getReason(), null);
    }

    /**
     * Handles all other uncaught exceptions.
     * Returns 500. Message is included for development — consider masking in production.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobal(Exception ex) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage(),
                null
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<T>> buildResponse(HttpStatus status, String message, T data) {
        ApiResponse<T> body = new ApiResponse<>(
                status.value(),
                message,
                LocalDateTime.now(),
                data
        );
        return ResponseEntity.status(status).body(body);
    }
}