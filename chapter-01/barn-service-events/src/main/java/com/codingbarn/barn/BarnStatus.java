package com.codingbarn.barn;

import java.time.Instant;

public record BarnStatus(String status, Instant fireStartedAt) {
    
    public static BarnStatus ok() {
        return new BarnStatus("OK", null);
    }
    
    public static BarnStatus onFire(Instant when) {
        return new BarnStatus("FIRE", when);
    }
    
    public boolean isOnFire() {
        return "FIRE".equals(status);
    }
}
