package com.codingbarn.firehouse;

import java.time.Instant;

/**
 * DTO matching the barn service's event structure.
 */
public record BarnEvent(String eventType, Instant timestamp, String barnId) {}
