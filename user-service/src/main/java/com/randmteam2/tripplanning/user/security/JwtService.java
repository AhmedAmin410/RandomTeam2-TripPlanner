package com.randmteam2.tripplanning.user.security;

import com.randmteam2.tripplanning.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private SecretKey key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(JwtConfigurationManager.getInstance().getSecret()));
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        long expMs = JwtConfigurationManager.getInstance().getExpirationMs();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .signWith(key())
                .compact();
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