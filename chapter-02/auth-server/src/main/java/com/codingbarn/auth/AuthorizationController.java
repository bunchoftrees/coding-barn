package com.codingbarn.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * OAuth 2.0 Authorization Server endpoint.
 * 
 * Implements the Client Credentials flow:
 * 1. Client sends credentials + requested scopes
 * 2. Server validates credentials
 * 3. Server checks if client is authorized for requested scopes
 * 4. Server issues JWT token with approved scopes
 */
@RestController
@RequestMapping("/oauth")
public class AuthorizationController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthorizationController.class);
    
    private final ClientRegistry clientRegistry;
    private final TokenService tokenService;
    
    public AuthorizationController(ClientRegistry clientRegistry, TokenService tokenService) {
        this.clientRegistry = clientRegistry;
        this.tokenService = tokenService;
    }
    
    @PostMapping("/token")
    public TokenResponse issueToken(@RequestBody TokenRequest request) {
        log.info("Token request from client: {}, scopes: {}", 
            request.clientId(), request.scopes());
        
        // Verify client credentials
        if (!clientRegistry.validateCredentials(request.clientId(), request.clientSecret())) {
            log.warn("Invalid credentials for client: {}", request.clientId());
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid client credentials"
            );
        }
        
        // Check what scopes this client is allowed to request
        Set<String> allowedScopes = clientRegistry.getAllowedScopes(request.clientId());
        Set<String> requestedScopes = request.scopes();
        
        if (!allowedScopes.containsAll(requestedScopes)) {
            Set<String> unauthorized = new HashSet<>(requestedScopes);
            unauthorized.removeAll(allowedScopes);
            log.warn("Client {} requested unauthorized scopes: {}", 
                request.clientId(), unauthorized);
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Client not authorized for scopes: " + unauthorized
            );
        }
        
        // Generate token with approved scopes
        String accessToken = tokenService.generateToken(
            request.clientId(),
            requestedScopes,
            Duration.ofHours(1)
        );
        
        log.info("Token issued to client: {} with scopes: {}", 
            request.clientId(), requestedScopes);
        
        return new TokenResponse(
            accessToken,
            "Bearer",
            3600, // expires in 1 hour
            requestedScopes
        );
    }
    
    @GetMapping("/health")
    public String health() {
        return "Auth server is running";
    }
}

record TokenRequest(
    String clientId,
    String clientSecret,
    Set<String> scopes
) {}

record TokenResponse(
    String accessToken,
    String tokenType,
    int expiresIn,
    Set<String> scopes
) {}
