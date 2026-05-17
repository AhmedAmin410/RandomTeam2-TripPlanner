package com.randmteam2.tripplanning.booking.security;

public class UserLoaderHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) throws AuthException {
        // In M3 each service has its own DB - users live in user-service
        // JWT signature already validated by SignatureValidationHandler
        // No cross-service DB lookup needed here
        if (ctx.claims == null || ctx.claims.getSubject() == null) {
            throw new AuthException(401, "Invalid token claims");
        }
        proceed(ctx);
    }
}
