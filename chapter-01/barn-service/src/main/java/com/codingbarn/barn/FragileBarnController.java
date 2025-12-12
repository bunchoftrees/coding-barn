package com.codingbarn.barn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A rate-limited barn that simulates fragile legacy systems.
 * 
 * This represents those mainframe systems that can't handle aggressive polling.
 * Poll too frequently and you risk destabilizing the entire system.
 * 
 * Maximum 6 requests per minute = you can poll every 10 seconds safely.
 * Poll faster than that and you'll start getting 503 errors.
 */
@RestController
@RequestMapping("/fragile-barn")
public class FragileBarnController {
    
    private static final Logger log = LoggerFactory.getLogger(FragileBarnController.class);
    
    private final AtomicReference<BarnStatus> status = 
        new AtomicReference<>(BarnStatus.ok());
    
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile Instant windowStart = Instant.now();
    
    // Maximum 6 requests per minute
    // This simulates a legacy system that can't handle frequent polling
    private static final int MAX_REQUESTS_PER_MINUTE = 6;
    
    /**
     * Get the current status - but don't ask too often!
     * 
     * This endpoint will start failing if you poll more than 6 times per minute.
     * Just like that old mainframe that starts throwing errors if you hit it too hard.
     */
    @GetMapping("/status")
    public BarnStatus getStatus() {
        Instant now = Instant.now();
        
        // Reset the rate limit window every minute
        if (now.isAfter(windowStart.plusSeconds(60))) {
            windowStart = now;
            requestCount.set(0);
            log.debug("Rate limit window reset");
        }
        
        int count = requestCount.incrementAndGet();
        
        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.error("⚠️ SYSTEM OVERLOAD! Request {} exceeds limit of {}/minute", 
                count, MAX_REQUESTS_PER_MINUTE);
            log.error("This is what happens when you poll a fragile system too aggressively.");
            
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "System overloaded. Too many status checks are destabilizing the barn. " +
                "Please reduce polling frequency to no more than once every 10 seconds."
            );
        }
        
        log.debug("Status check {}/{} this minute", count, MAX_REQUESTS_PER_MINUTE);
        return status.get();
    }
    
    /**
     * Start a fire in the fragile barn.
     */
    @PostMapping("/ignite")
    public String startFire() {
        Instant fireTime = Instant.now();
        status.set(new BarnStatus("FIRE", fireTime));
        log.warn("🔥 Fire started in fragile barn at {}", fireTime);
        return "Fire started at " + fireTime;
    }
    
    /**
     * Extinguish the fire.
     */
    @PostMapping("/extinguish")
    public String extinguish() {
        status.set(BarnStatus.ok());
        log.info("✓ Fragile barn fire extinguished");
        return "Fire extinguished";
    }
    
    /**
     * Check the current rate limit status.
     */
    @GetMapping("/rate-limit")
    public RateLimitStatus getRateLimitStatus() {
        Instant now = Instant.now();
        long secondsRemaining = 60 - java.time.Duration.between(windowStart, now).toSeconds();
        if (secondsRemaining < 0) secondsRemaining = 60;
        
        return new RateLimitStatus(
            requestCount.get(),
            MAX_REQUESTS_PER_MINUTE,
            secondsRemaining
        );
    }
    
    public record RateLimitStatus(int requestsThisMinute, int maxPerMinute, long secondsUntilReset) {}
}
