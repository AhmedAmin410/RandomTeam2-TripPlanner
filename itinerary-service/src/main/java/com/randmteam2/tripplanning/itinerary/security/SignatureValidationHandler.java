package com.randmteam2.tripplanning.itinerary.security;

public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (!jwtService.isTokenValid(ctx.getToken())) {
            throw new AuthException("Invalid or expired JWT token", 401);
        }
        ctx.setEmail(jwtService.extractEmail(ctx.getToken()));
        ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
        ctx.setRole(jwtService.extractRole(ctx.getToken()));
        if (next != null) next.handle(ctx);
    }
}
