package com.randmteam2.tripplanning.itinerary.security;

public class UserLoaderHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (ctx.getEmail() == null || ctx.getEmail().isBlank()) {
            throw new AuthException("Token missing subject claim", 401);
        }
        if (next != null) next.handle(ctx);
    }
}
