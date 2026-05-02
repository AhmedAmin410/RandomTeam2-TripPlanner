package com.randomteam2.tripplanning.destination.security;

import jakarta.servlet.http.HttpServletResponse;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    protected void doHandle(AuthContext context) {
        String authorizationHeader = context.getRequest().getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationException(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
        }

        context.setToken(authorizationHeader.substring(7));
    }
}