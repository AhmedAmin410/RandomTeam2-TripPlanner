package com.randmteam2.tripplanning.itinerary.security;

import com.randmteam2.tripplanning.itinerary.security.AuthContext;
import com.randmteam2.tripplanning.itinerary.security.AuthException;
import com.randmteam2.tripplanning.itinerary.security.AuthHandler;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        try {
            ctx.claims = jwtService.extractAllClaims(ctx.token);
        } catch (Exception e) {
            throw new AuthException(401, "Invalid or expired token");
        }
        proceed(ctx);
    }
}