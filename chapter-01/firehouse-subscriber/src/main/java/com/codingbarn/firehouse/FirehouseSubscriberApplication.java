package com.codingbarn.firehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class FirehouseSubscriberApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirehouseSubscriberApplication.class, args);
    }
}

/**
 * Automatically subscribes to barn events when the application starts.
 * 
 * This is like installing a smoke detector - once it's set up,
 * the fire station will be notified automatically whenever there's a fire.
 */
@Component
class SubscriptionInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionInitializer.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${barn.service.url:http://localhost:8082}")
    private String barnServiceUrl;
    
    @Value("${server.port:8083}")
    private int serverPort;
    
    @Value("${firehouse.callback.host:localhost}")
    private String callbackHost;
    
    @EventListener(ApplicationReadyEvent.class)
    public void subscribeToBar() {
        String callbackUrl = "http://" + callbackHost + ":" + serverPort + "/events";
        String subscribeUrl = barnServiceUrl + "/barn/subscribe?callbackUrl=" + callbackUrl;
        
        log.info("════════════════════════════════════════════════════");
        log.info("🏠 Firehouse Subscriber starting up");
        log.info("════════════════════════════════════════════════════");
        log.info("Registering webhook with barn service...");
        log.info("  Barn URL: {}", barnServiceUrl);
        log.info("  Callback URL: {}", callbackUrl);
        
        try {
            String response = restTemplate.postForObject(subscribeUrl, null, String.class);
            log.info("✓ Successfully subscribed to barn events!");
            log.info("  Response: {}", response);
            log.info("");
            log.info("The fire station is now connected.");
            log.info("When the barn catches fire, we'll know instantly.");
            log.info("════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("✗ Failed to subscribe to barn events: {}", e.getMessage());
            log.error("  Make sure barn-service-events is running on {}", barnServiceUrl);
            log.error("════════════════════════════════════════════════════");
        }
    }
}
