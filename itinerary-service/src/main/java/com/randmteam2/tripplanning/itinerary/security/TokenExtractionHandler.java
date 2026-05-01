package com.randmteam2.tripplanning.itinerary.security;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        String header = ctx.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthException("Missing or malformed Authorization header", 401);
        }
        ctx.setToken(header.substring(7));
        if (next != null) next.handle(ctx);
    }
}