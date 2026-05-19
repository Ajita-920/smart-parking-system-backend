package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping(ApiConstant.HEALTH_BASE)
public class HealthController extends BaseController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "Smart Parking System",
                "timestamp", LocalDateTime.now().toString()
        );
        return okResponse("Service is running", status);
    }
}
