package com.randmteam2.tripplanning.itinerary.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;

@Service
public class JwtService {

    private final Key signingKey;

    public JwtService() {
        JwtConfigurationManager config = JwtConfigurationManager.getInstance();
        byte[] keyBytes = Base64.getDecoder().decode(config.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = extractAllClaims(token).get("uid");
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        if (uid instanceof Long) return (Long) uid;
        return Long.valueOf(uid.toString());
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
