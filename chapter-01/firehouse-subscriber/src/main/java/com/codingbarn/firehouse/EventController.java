package com.codingbarn.firehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Webhook endpoint for receiving barn events.
 * 
 * This is the "smoke detector" end of the system.
 * When the barn catches fire, it calls this endpoint immediately.
 * No polling. No delay. Instant notification.
 */
@RestController
public class EventController {
    
    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    
    // Statistics
    private final AtomicInteger eventsReceived = new AtomicInteger(0);
    private final AtomicInteger firesDetected = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final List<EventRecord> recentEvents = new ArrayList<>();
    
    /**
     * Receive events from the barn.
     * 
     * This is called by the barn service when something happens.
     * We don't ask for updates - they're pushed to us.
     */
    @PostMapping("/events")
    public String handleEvent(@RequestBody BarnEvent event) {
        Instant receivedAt = Instant.now();
        eventsReceived.incrementAndGet();
        
        Duration responseTime = Duration.between(event.timestamp(), receivedAt);
        
        // Store for history
        synchronized (recentEvents) {
            recentEvents.add(new EventRecord(event, receivedAt, responseTime.toMillis()));
            // Keep only last 100 events
            if (recentEvents.size() > 100) {
                recentEvents.remove(0);
            }
        }
        
        if ("FIRE".equals(event.eventType())) {
            firesDetected.incrementAndGet();
            totalResponseTimeMs.addAndGet(responseTime.toMillis());
            
            log.error("═══════════════════════════════════════════════════");
            log.error("🔥🔥🔥 FIRE EVENT RECEIVED! 🔥🔥🔥");
            log.error("═══════════════════════════════════════════════════");
            log.error("Barn ID:        {}", event.barnId());
            log.error("Fire started:   {}", event.timestamp());
            log.error("Event received: {}", receivedAt);
            log.error("Response time:  {} milliseconds", responseTime.toMillis());
            log.error("═══════════════════════════════════════════════════");
            log.error("");
            log.error("That's {} MILLISECONDS, not seconds.", responseTime.toMillis());
            log.error("The barn told us instantly. No polling required.");
            log.error("");
            
        } else if ("EXTINGUISHED".equals(event.eventType())) {
            log.info("════════════════════════════════════════════════════");
            log.info("✓ Fire extinguished at barn: {}", event.barnId());
            log.info("  Event received in {} ms", responseTime.toMillis());
            log.info("════════════════════════════════════════════════════");
            
        } else {
            log.info("Event received: {} from {} ({}ms)", 
                event.eventType(), event.barnId(), responseTime.toMillis());
        }
        
        return "Event processed in " + responseTime.toMillis() + "ms";
    }
    
    /**
     * Get statistics about received events.
     */
    @GetMapping("/stats")
    public EventStats getStats() {
        long avgResponseTime = firesDetected.get() > 0 
            ? totalResponseTimeMs.get() / firesDetected.get() 
            : 0;
            
        return new EventStats(
            eventsReceived.get(),
            firesDetected.get(),
            avgResponseTime
        );
    }
    
    /**
     * Get recent event history.
     */
    @GetMapping("/events/history")
    public List<EventRecord> getHistory() {
        synchronized (recentEvents) {
            return new ArrayList<>(recentEvents);
        }
    }
    
    public record EventStats(int totalEvents, int firesDetected, long avgResponseTimeMs) {}
    
    public record EventRecord(BarnEvent event, Instant receivedAt, long responseTimeMs) {}
}
