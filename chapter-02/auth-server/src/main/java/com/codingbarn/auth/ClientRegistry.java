package com.codingbarn.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory client registry.
 * In production, this would be a database with bcrypt-hashed secrets.
 * 
 * Pre-registered clients:
 * - harvest-service: Can only read "now playing" info
 * - party-guest-app: Can read and control music
 * - admin-app: Can do everything including delete equipment
 */
@Component
public class ClientRegistry {
    
    private final Map<String, RegisteredClient> clients = new ConcurrentHashMap<>();
    
    public ClientRegistry() {
        // Register harvest-service with read-only scope
        register(new RegisteredClient(
            "harvest-service",
            "harvest-secret-key",
            Set.of("read:nowplaying")
        ));
        
        // Register party-guest-app with read and write scopes
        register(new RegisteredClient(
            "party-guest-app",
            "party-secret-key",
            Set.of("read:nowplaying", "write:music")
        ));
        
        // Register admin-app with all scopes
        register(new RegisteredClient(
            "admin-app",
            "admin-secret-key",
            Set.of("read:nowplaying", "write:music", "admin:equipment")
        ));
    }
    
    private void register(RegisteredClient client) {
        clients.put(client.clientId(), client);
    }
    
    public boolean validateCredentials(String clientId, String clientSecret) {
        RegisteredClient client = clients.get(clientId);
        if (client == null) {
            return false;
        }
        // In production: use bcrypt or similar
        return client.clientSecret().equals(clientSecret);
    }
    
    public Set<String> getAllowedScopes(String clientId) {
        RegisteredClient client = clients.get(clientId);
        return client != null ? client.allowedScopes() : Set.of();
    }
    
    public record RegisteredClient(
        String clientId,
        String clientSecret,
        Set<String> allowedScopes
    ) {}
}
