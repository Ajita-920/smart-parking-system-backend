package com.projectwork.Smart.Parking.System.service;

import java.util.Map;

/**
 * Service health reporting contract.
 */
public interface HealthService {

    /**
     * Returns a small map of health metadata for monitoring.
     */
    Map<String, Object> getHealthStatus();
}
