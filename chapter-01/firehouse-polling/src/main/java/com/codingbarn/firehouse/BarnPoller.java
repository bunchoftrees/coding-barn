package com.codingbarn.firehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls the barn service for status on a fixed interval.
 * 
 * This is the "old way" - we have to keep asking "are you on fire?"
 * because the barn can't tell us when something happens.
 * 
 * Watch the logs and notice:
 * - How many checks happen when nothing is wrong (wasted effort)
 * - The delay between fire starting and fire detection
 * - What happens if we poll too aggressively (fragile system)
 */
@Component
public class BarnPoller {
    
    private static final Logger log = LoggerFactory.getLogger(BarnPoller.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${barn.service.url:http://localhost:8080}")
    private String barnServiceUrl;
    
    @Value("${barn.endpoint:/barn/status}")
    private String barnEndpoint;
    
    @Value("${polling.interval.ms:10000}")
    private int pollingIntervalMs;
    
    // Track when we detected the fire (to calculate response time)
    private final AtomicReference<Instant> fireDetectedAt = new AtomicReference<>(null);
    
    // Statistics
    private final AtomicInteger totalPolls = new AtomicInteger(0);
    private final AtomicInteger successfulPolls = new AtomicInteger(0);
    private final AtomicInteger failedPolls = new AtomicInteger(0);
    private final AtomicInteger firesDetected = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    
    @Scheduled(fixedRateString = "${polling.interval.ms:10000}")
    public void checkBarn() {
        totalPolls.incrementAndGet();
        String url = barnServiceUrl + barnEndpoint;
        
        try {
            BarnStatus status = restTemplate.getForObject(url, BarnStatus.class);
            successfulPolls.incrementAndGet();
            
            if (status == null) {
                log.warn("Received null status from barn");
                return;
            }
            
            log.info("📋 Polling barn... Status: {}", status.status());
            
            if (status.isOnFire() && fireDetectedAt.get() == null) {
                // Fire detected!
                Instant detected = Instant.now();
                fireDetectedAt.set(detected);
                firesDetected.incrementAndGet();
                
                Duration responseTime = Duration.between(status.fireStartedAt(), detected);
                totalResponseTimeMs.addAndGet(responseTime.toMillis());
                
                log.error("═══════════════════════════════════════════════════");
                log.error("🔥🔥🔥 FIRE DETECTED! 🔥🔥🔥");
                log.error("═══════════════════════════════════════════════════");
                log.error("Fire started at:  {}", status.fireStartedAt());
                log.error("Fire detected at: {}", detected);
                log.error("Response time:    {} seconds ({} ms)", 
                    responseTime.toSeconds(), responseTime.toMillis());
                log.error("═══════════════════════════════════════════════════");
                log.error("");
                log.error("The barn burned for {} seconds before we noticed.", 
                    responseTime.toSeconds());
                log.error("With a polling interval of {} ms, this is expected.", 
                    pollingIntervalMs);
                log.error("");
                
            } else if (!status.isOnFire() && fireDetectedAt.get() != null) {
                // Fire was extinguished
                fireDetectedAt.set(null);
                log.info("✓ Fire has been extinguished. Resuming normal monitoring.");
            }
            
        } catch (Exception e) {
            failedPolls.incrementAndGet();
            log.warn("❌ Failed to reach barn: {}", e.getMessage());
            
            if (e.getMessage() != null && e.getMessage().contains("503")) {
                log.warn("The barn service is overwhelmed by our polling.");
                log.warn("This is what happens with fragile legacy systems.");
            }
        }
    }
    
    // Expose statistics
    public PollingStats getStats() {
        return new PollingStats(
            totalPolls.get(),
            successfulPolls.get(),
            failedPolls.get(),
            firesDetected.get(),
            firesDetected.get() > 0 
                ? totalResponseTimeMs.get() / firesDetected.get() 
                : 0,
            pollingIntervalMs
        );
    }
    
    public record PollingStats(
        int totalPolls,
        int successfulPolls,
        int failedPolls,
        int firesDetected,
        long avgResponseTimeMs,
        int pollingIntervalMs
    ) {}
    
    public record BarnStatus(String status, Instant fireStartedAt) {
        public boolean isOnFire() {
            return "FIRE".equals(status);
        }
    }
}
