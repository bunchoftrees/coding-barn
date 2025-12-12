package com.codingbarn.firehouse;

import java.time.Instant;

/**
 * DTO matching the barn service's status response.
 */
public record BarnStatus(String status, Instant fireStartedAt) {
    
    public boolean isOnFire() {
        return "FIRE".equals(status);
    }
}
