package com.codingbarn.barn;

import java.time.Instant;

/**
 * Represents the current state of a barn.
 * 
 * @param status Current status: "OK" or "FIRE"
 * @param fireStartedAt When the fire started (null if no fire)
 */
public record BarnStatus(String status, Instant fireStartedAt) {
    
    public static BarnStatus ok() {
        return new BarnStatus("OK", null);
    }
    
    public static BarnStatus onFire() {
        return new BarnStatus("FIRE", Instant.now());
    }
    
    public boolean isOnFire() {
        return "FIRE".equals(status);
    }
}
