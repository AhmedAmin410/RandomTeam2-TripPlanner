package com.randmteam2.tripplanning.user.security;

import com.randmteam2.tripplanning.user.security.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private SecretKey key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(JwtConfigurationManager.getInstance().getSecret()));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token)  { return extractAllClaims(token).getSubject(); }
    public Long   extractUserId(String token) { return extractAllClaims(token).get("uid", Long.class); }
    public String extractRole(String token)   { return extractAllClaims(token).get("role", String.class); }

    public boolean isTokenValid(String token) {
        try { extractAllClaims(token); return true; }
        catch (Exception e) { return false; }
    }
}