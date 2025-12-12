package com.codingbarn.barn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST controller for an event-driven barn.
 * 
 * This is the "modern way" - when something happens (like a fire),
 * the barn immediately notifies all subscribers. No one has to poll.
 * 
 * The fire station just registers a webhook, and the barn calls it
 * the instant something goes wrong.
 */
@RestController
@RequestMapping("/barn")
public class EventBarnController {
    
    private static final Logger log = LoggerFactory.getLogger(EventBarnController.class);
    
    private final AtomicReference<BarnStatus> status = 
        new AtomicReference<>(BarnStatus.ok());
    
    private final List<String> subscribers = new CopyOnWriteArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${barn.id:main-barn}")
    private String barnId;
    
    /**
     * Get the current status.
     * Still available for manual checks, but subscribers don't need to poll.
     */
    @GetMapping("/status")
    public BarnStatus getStatus() {
        return status.get();
    }
    
    /**
     * Register a webhook URL to receive events.
     * 
     * This is how the fire station "installs a smoke detector."
     * Once registered, they'll be notified instantly when something happens.
     */
    @PostMapping("/subscribe")
    public SubscriptionResponse subscribe(@RequestParam String callbackUrl) {
        if (!subscribers.contains(callbackUrl)) {
            subscribers.add(callbackUrl);
            log.info("✓ New subscriber registered: {}", callbackUrl);
            log.info("  Total subscribers: {}", subscribers.size());
        } else {
            log.info("Subscriber already registered: {}", callbackUrl);
        }
        return new SubscriptionResponse(callbackUrl, true, subscribers.size());
    }
    
    /**
     * Unsubscribe from events.
     */
    @PostMapping("/unsubscribe")
    public SubscriptionResponse unsubscribe(@RequestParam String callbackUrl) {
        boolean removed = subscribers.remove(callbackUrl);
        if (removed) {
            log.info("✓ Subscriber removed: {}", callbackUrl);
        }
        return new SubscriptionResponse(callbackUrl, false, subscribers.size());
    }
    
    /**
     * List all current subscribers.
     */
    @GetMapping("/subscribers")
    public List<String> getSubscribers() {
        return List.copyOf(subscribers);
    }
    
    /**
     * Start a fire in the barn.
     * 
     * Unlike the polling version, this immediately notifies all subscribers.
     * The fire station knows about the fire within milliseconds.
     */
    @PostMapping("/ignite")
    public String startFire() {
        Instant fireTime = Instant.now();
        status.set(BarnStatus.onFire(fireTime));
        
        log.warn("═══════════════════════════════════════════════════");
        log.warn("🔥 Fire started at {}", fireTime);
        log.warn("═══════════════════════════════════════════════════");
        
        // Immediately notify all subscribers
        BarnEvent event = BarnEvent.fire(barnId);
        int notified = notifySubscribers(event);
        
        log.warn("📢 Notified {} subscribers immediately", notified);
        log.warn("No waiting. No polling. Instant notification.");
        
        return "Fire started at " + fireTime + ". Notified " + notified + " subscribers.";
    }
    
    /**
     * Extinguish the fire.
     */
    @PostMapping("/extinguish")
    public String extinguish() {
        status.set(BarnStatus.ok());
        
        BarnEvent event = BarnEvent.extinguished(barnId);
        int notified = notifySubscribers(event);
        
        log.info("✓ Fire extinguished. Notified {} subscribers.", notified);
        return "Fire extinguished. Notified " + notified + " subscribers.";
    }
    
    /**
     * Push an event to all registered subscribers.
     * 
     * In a production system, you'd want:
     * - Async processing (don't block the main request)
     * - Retry logic with exponential backoff
     * - Dead letter queue for failed deliveries
     * - Circuit breaker for misbehaving subscribers
     * 
     * But this is a toy - we're keeping it simple to show the concept.
     */
    private int notifySubscribers(BarnEvent event) {
        int successful = 0;
        
        for (String subscriberUrl : subscribers) {
            try {
                restTemplate.postForObject(subscriberUrl, event, String.class);
                log.debug("  ✓ Notified: {}", subscriberUrl);
                successful++;
            } catch (Exception e) {
                log.warn("  ✗ Failed to notify {}: {}", subscriberUrl, e.getMessage());
                // In production: queue for retry, don't just drop it
            }
        }
        
        return successful;
    }
    
    public record SubscriptionResponse(String callbackUrl, boolean subscribed, int totalSubscribers) {}
}
