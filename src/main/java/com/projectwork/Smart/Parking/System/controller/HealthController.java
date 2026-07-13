package com.projectwork.Smart.Parking.System.controller;

import com.projectwork.Smart.Parking.System.config.ApiConstant;
import com.projectwork.Smart.Parking.System.dto.ApiResponse;
import com.projectwork.Smart.Parking.System.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight health endpoint used by monitoring and local smoke checks.
 */
@RestController
@RequestMapping(ApiConstant.HEALTH_BASE)
public class HealthController extends BaseController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Returns basic service health metadata.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return okResponse("Service is running", healthService.getHealthStatus());
    }
}
