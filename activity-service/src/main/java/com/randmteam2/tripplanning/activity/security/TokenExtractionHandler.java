package com.randmteam2.tripplanning.activity.security;

import jakarta.servlet.http.HttpServletResponse;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext context) {
        String header = context.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            context.fail(HttpServletResponse.SC_UNAUTHORIZED, "Missing or malformed Authorization header");
            return;
        }
        context.setToken(header.substring(7));
        proceed(context);
    }
}
