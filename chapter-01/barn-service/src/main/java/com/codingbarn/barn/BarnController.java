package com.codingbarn.barn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST controller for a barn that can catch fire.
 * 
 * This is the "polling" version - the barn just sits here and answers
 * questions about its status. It never volunteers information.
 * If you want to know if it's on fire, you have to ask.
 */
@RestController
@RequestMapping("/barn")
public class BarnController {
    
    private static final Logger log = LoggerFactory.getLogger(BarnController.class);
    
    private final AtomicReference<BarnStatus> status = 
        new AtomicReference<>(BarnStatus.ok());
    
    /**
     * Get the current status of the barn.
     * This is what polling clients call repeatedly.
     */
    @GetMapping("/status")
    public BarnStatus getStatus() {
        BarnStatus current = status.get();
        log.debug("Status check: {}", current.status());
        return current;
    }
    
    /**
     * Start a fire in the barn.
     * The barn doesn't tell anyone - it just updates its internal state.
     * Someone has to poll /status to find out.
     */
    @PostMapping("/ignite")
    public String startFire() {
        Instant fireTime = Instant.now();
        status.set(new BarnStatus("FIRE", fireTime));
        log.warn("🔥 Fire started at {}", fireTime);
        log.warn("The barn is burning silently. No one has been notified.");
        return "Fire started at " + fireTime;
    }
    
    /**
     * Extinguish the fire.
     */
    @PostMapping("/extinguish")
    public String extinguish() {
        status.set(BarnStatus.ok());
        log.info("✓ Fire extinguished");
        return "Fire extinguished";
    }
}
