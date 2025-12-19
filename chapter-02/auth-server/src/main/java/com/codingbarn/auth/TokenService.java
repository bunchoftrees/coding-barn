package com.codingbarn.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

/**
 * Generates JWT tokens with client ID and scopes.
 * 
 * In production:
 * - Use a proper key management system
 * - Rotate keys regularly
 * - Consider using asymmetric keys (RS256) instead of symmetric (HS256)
 */
@Service
public class TokenService {
    
    // In production: Load from secure config, rotate regularly
    private static final String SECRET = "this-is-a-very-long-secret-key-for-jwt-signing-at-least-256-bits-long";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    public String generateToken(String clientId, Set<String> scopes, Duration expiresIn) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expiresIn);
        
        return Jwts.builder()
            .subject(clientId)
            .claim("scopes", scopes)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }
    
    public SecretKey getSigningKey() {
        return signingKey;
    }
}
