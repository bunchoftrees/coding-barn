package com.codingbarn.barn;

import java.time.Instant;

/**
 * An event emitted by the barn when something significant happens.
 * 
 * @param eventType Type of event: FIRE, EXTINGUISHED, etc.
 * @param timestamp When the event occurred
 * @param barnId Identifier for the barn (for multi-barn scenarios)
 */
public record BarnEvent(String eventType, Instant timestamp, String barnId) {
    
    public static BarnEvent fire(String barnId) {
        return new BarnEvent("FIRE", Instant.now(), barnId);
    }
    
    public static BarnEvent extinguished(String barnId) {
        return new BarnEvent("EXTINGUISHED", Instant.now(), barnId);
    }
}
