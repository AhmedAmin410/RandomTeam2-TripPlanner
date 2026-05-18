package com.randmteam2.tripplanning.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Validates JWT for routed APIs (WebFlux), forwards identity and correlation headers, and skips
 * validation for auth routes, public user registration/login, service health, and actuator.
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String X_CORRELATION_ID = "X-Correlation-ID";
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USER_ROLE = "X-User-Role";

    private final SecretKey signingKey;

    public JwtGatewayFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret.trim()));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String correlationId = correlationId(exchange);

        if (shouldBypassJwt(path)) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(X_CORRELATION_ID, correlationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        String auth = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER)) {
            return unauthorized(exchange);
        }
        String token = auth.substring(BEARER.length()).trim();
        if (token.isEmpty()) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long uid = claims.get("uid", Long.class);
            String role = claims.get("role", String.class);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(X_CORRELATION_ID, correlationId)
                    .header(X_USER_ID, uid != null ? String.valueOf(uid) : "")
                    .header(X_USER_ROLE, role != null ? role : "")
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private static String correlationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(X_CORRELATION_ID);
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * /api/auth/**, public registration/login, all service /api/*/health probes, and actuator.
     */
    static boolean shouldBypassJwt(String path) {
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/actuator/")) {
            return true;
        }
        return path.matches("/api/(users|destinations|itineraries|activities|bookings)/health")
                || "/api/users/register".equals(path)
                || "/api/users/login".equals(path);
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
