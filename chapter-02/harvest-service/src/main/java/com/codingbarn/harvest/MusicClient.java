package com.codingbarn.harvest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Set;

/**
 * OAuth client that fetches music data from shed-service.
 * 
 * This service:
 * 1. Requests tokens from auth-server using client credentials
 * 2. Caches tokens until they expire
 * 3. Makes authenticated calls to shed-service
 * 4. Exposes the data through public endpoints
 */
@Service
public class MusicClient {
    
    private static final Logger log = LoggerFactory.getLogger(MusicClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${auth.server.url:http://localhost:8081}")
    private String authServerUrl;
    
    @Value("${shed.service.url:http://localhost:8080}")
    private String shedServiceUrl;
    
    @Value("${oauth.client.id:harvest-service}")
    private String clientId;
    
    @Value("${oauth.client.secret:harvest-secret-key}")
    private String clientSecret;
    
    private String cachedToken;
    private Instant tokenExpiry;
    
    public MusicClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public Song getCurrentSong() {
        String token = getValidToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Song> response = restTemplate.exchange(
            shedServiceUrl + "/music/nowplaying",
            HttpMethod.GET,
            entity,
            Song.class
        );
        
        return response.getBody();
    }
    
    private String getValidToken() {
        // If token is still valid, reuse it
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            log.debug("Reusing cached token");
            return cachedToken;
        }
        
        // Otherwise, request a new one
        log.info("Requesting new token from auth server");
        return requestNewToken();
    }
    
    private String requestNewToken() {
        TokenRequest request = new TokenRequest(
            clientId,
            clientSecret,
            Set.of("read:nowplaying")
        );
        
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            authServerUrl + "/oauth/token",
            request,
            TokenResponse.class
        );
        
        TokenResponse tokenResponse = response.getBody();
        if (tokenResponse == null) {
            throw new RuntimeException("Failed to get token from auth server");
        }
        
        cachedToken = tokenResponse.accessToken();
        // Set expiry slightly before actual expiry to avoid edge cases
        tokenExpiry = Instant.now().plusSeconds(tokenResponse.expiresIn() - 60);
        
        log.info("Token acquired, expires in {} seconds", tokenResponse.expiresIn());
        
        return cachedToken;
    }
}

record TokenRequest(String clientId, String clientSecret, Set<String> scopes) {}
record TokenResponse(String accessToken, String tokenType, int expiresIn, Set<String> scopes) {}
record Song(String id, String title, String artist, String album) {}
