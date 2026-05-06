package com.randmteam2.tripplanning.activity.security;

import com.randmteam2.tripplanning.activity.security.AuthContext;
import com.randmteam2.tripplanning.activity.security.AuthException;
import com.randmteam2.tripplanning.activity.security.AuthHandler;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) throws AuthException {
        String header = ctx.request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthException(401, "Missing or malformed Authorization header");
        }
        ctx.token = header.substring(7);
        proceed(ctx);
    }
}