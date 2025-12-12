package com.codingbarn.firehouse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes statistics about our polling behavior.
 */
@RestController
public class StatsController {
    
    private final BarnPoller barnPoller;
    
    public StatsController(BarnPoller barnPoller) {
        this.barnPoller = barnPoller;
    }
    
    @GetMapping("/stats")
    public BarnPoller.PollingStats getStats() {
        return barnPoller.getStats();
    }
}
