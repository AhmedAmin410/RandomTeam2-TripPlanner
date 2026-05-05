package com.randmteam2.tripplanning.activity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService() {
        JwtConfigurationManager cfg = JwtConfigurationManager.getInstance();
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(cfg.getSecret()));
        this.expirationMs = cfg.getExpirationMs();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) { return extractAllClaims(token).getSubject(); }

    public Long extractUserId(String token) {
        Object uid = extractAllClaims(token).get("uid");
        if (uid instanceof Integer) return ((Integer) uid).longValue();
        if (uid instanceof Long) return (Long) uid;
        return Long.parseLong(uid.toString());
    }

    public String extractRole(String token) { return (String) extractAllClaims(token).get("role"); }

    public boolean isTokenValid(String token) {
        try { return extractAllClaims(token).getExpiration().after(new Date()); }
        catch (Exception e) { return false; }
    }
}
