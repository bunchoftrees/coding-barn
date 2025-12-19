package com.codingbarn.shed;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Validates JWT tokens issued by the auth server.
 * 
 * In production:
 * - The secret key should come from secure config
 * - Should match the key used by auth-server
 * - Consider using public key validation (RS256) instead
 */
@Service
public class TokenValidator {
    
    // Must match the secret in auth-server
    private static final String SECRET = "this-is-a-very-long-secret-key-for-jwt-signing-at-least-256-bits-long";
    private final SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    public TokenInfo validate(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
            
            Claims body = claims.getPayload();
            
            // Check expiration
            if (body.getExpiration().before(new Date())) {
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token has expired"
                );
            }
            
            // Extract scopes from claims
            @SuppressWarnings("unchecked")
            List<String> scopesList = body.get("scopes", List.class);
            Set<String> scopes = scopesList != null ? Set.copyOf(scopesList) : Set.of();
            
            return new TokenInfo(
                body.getSubject(), // subject (often a user ID; for client credentials you might set this to the client ID)
                scopes,
                body.getExpiration().toInstant()
            );
            
        } catch (JwtException e) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid token: " + e.getMessage()
            );
        }
    }
}

record TokenInfo(
    String clientId,
    Set<String> scopes,
    Instant expiration
) {
    public boolean hasScope(String requiredScope) {
        return scopes.contains(requiredScope);
    }
}
