package com.codingbarn.guest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

/**
 * Party guest app that demonstrates:
 * 1. Requesting OAuth tokens
 * 2. Using tokens to access shed-service
 * 3. What happens when you try to exceed your scope
 * 
 * This client has read:nowplaying and write:music scopes.
 * It can control music but cannot delete equipment.
 */
@RestController
@RequestMapping("/guest")
public class GuestController {
    
    private static final Logger log = LoggerFactory.getLogger(GuestController.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${auth.server.url:http://localhost:8081}")
    private String authServerUrl;
    
    @Value("${shed.service.url:http://localhost:8080}")
    private String shedServiceUrl;
    
    @Value("${oauth.client.id:party-guest-app}")
    private String clientId;
    
    @Value("${oauth.client.secret:party-secret-key}")
    private String clientSecret;
    
    private String currentToken;
    
    public GuestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Request a token with specific scopes.
     * Returns the token for experimentation.
     */
    @PostMapping("/token")
    public TokenInfo requestToken(@RequestBody TokenRequestParams params) {
        log.info("Requesting token with scopes: {}", params.scopes());
        
        TokenRequest request = new TokenRequest(
            clientId,
            clientSecret,
            params.scopes()
        );
        
        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                authServerUrl + "/oauth/token",
                request,
                TokenResponse.class
            );
            
            TokenResponse tokenResponse = response.getBody();
            currentToken = tokenResponse.accessToken();
            
            log.info("Token acquired with scopes: {}", tokenResponse.scopes());
            
            return new TokenInfo(
                currentToken,
                tokenResponse.scopes(),
                tokenResponse.expiresIn(),
                "Token acquired! Use it in subsequent requests."
            );
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to get token: {}", e.getMessage());
            return new TokenInfo(
                null,
                Set.of(),
                0,
                "Failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * View currently playing song.
     * Requires scope: read:nowplaying
     */
    @GetMapping("/nowplaying")
    public Object viewNowPlaying() {
        if (currentToken == null) {
            return "No token! Request one first with POST /guest/token";
        }
        
        log.info("Viewing now playing with token");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(currentToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Song> response = restTemplate.exchange(
                shedServiceUrl + "/music/nowplaying",
                HttpMethod.GET,
                entity,
                Song.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to access: {}", e.getMessage());
            return "Failed: " + e.getStatusCode() + " - " + e.getMessage();
        }
    }
    
    /**
     * Change the song.
     * Requires scope: write:music
     */
    @PostMapping("/play/{songId}")
    public Object playSong(@PathVariable String songId) {
        if (currentToken == null) {
            return "No token! Request one first with POST /guest/token";
        }
        
        log.info("Attempting to play song {} with token", songId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(currentToken);
        HttpEntity<PlayRequest> entity = new HttpEntity<>(new PlayRequest(songId), headers);
        
        try {
            ResponseEntity<Song> response = restTemplate.exchange(
                shedServiceUrl + "/music/play",
                HttpMethod.POST,
                entity,
                Song.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to play song: {}", e.getMessage());
            return "Failed: " + e.getStatusCode() + " - " + e.getMessage();
        }
    }
    
    /**
     * Try to delete all equipment (should fail - we don't have admin:equipment scope).
     * This demonstrates scope enforcement.
     */
    @DeleteMapping("/equipment")
    public Object tryToDeleteEquipment() {
        if (currentToken == null) {
            return "No token! Request one first with POST /guest/token";
        }
        
        log.warn("Attempting to delete equipment (should fail!)");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(currentToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                shedServiceUrl + "/music/equipment",
                HttpMethod.DELETE,
                entity,
                String.class
            );
            
            log.error("⚠️ UNEXPECTED: Equipment deletion succeeded!");
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.info("✓ Expected failure: {}", e.getMessage());
            return "Expected failure: " + e.getStatusCode() + " - Scope enforcement working!";
        }
    }
    
    @GetMapping("/health")
    public String health() {
        return "Party guest app is running";
    }
}

record TokenRequest(String clientId, String clientSecret, Set<String> scopes) {}
record TokenResponse(String accessToken, String tokenType, int expiresIn, Set<String> scopes) {}
record TokenRequestParams(Set<String> scopes) {}
record TokenInfo(String token, Set<String> scopes, int expiresIn, String message) {}
record Song(String id, String title, String artist, String album) {}
record PlayRequest(String songId) {}
