package com.projectwork.Smart.Parking.System.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public Map<String, Object> getHealthStatus() {
        return Map.of(
                "status", "UP",
                "service", "Smart Parking System",
                "timestamp", Instant.now().toString());
    }
}
