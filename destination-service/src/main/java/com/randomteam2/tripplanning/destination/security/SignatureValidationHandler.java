package com.randomteam2.tripplanning.destination.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;

public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doHandle(AuthContext context) {
        try {
            Claims claims = jwtService.validateAndExtractClaims(context.getToken());
            context.setClaims(claims);
            context.setEmail(claims.getSubject());

            Object uidClaim = claims.get("uid");
            if (uidClaim instanceof Number number) {
                context.setUserId(number.longValue());
            }

            Object roleClaim = claims.get("role");
            if (roleClaim != null) {
                context.setRole(roleClaim.toString());
            }
        } catch (Exception exception) {
            throw new AuthenticationException(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}