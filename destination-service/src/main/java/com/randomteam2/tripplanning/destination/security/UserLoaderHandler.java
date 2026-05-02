package com.randomteam2.tripplanning.destination.security;

import jakarta.servlet.http.HttpServletResponse;

public class UserLoaderHandler extends AuthHandler {

    @Override
    protected void doHandle(AuthContext context) {
        if (context.getEmail() == null || context.getEmail().isBlank()) {
            throw new AuthenticationException(HttpServletResponse.SC_UNAUTHORIZED, "Token subject is missing");
        }

        if (context.getUserId() == null) {
            throw new AuthenticationException(HttpServletResponse.SC_UNAUTHORIZED, "Token uid is missing");
        }

        if (context.getRole() == null || context.getRole().isBlank()) {
            throw new AuthenticationException(HttpServletResponse.SC_UNAUTHORIZED, "Token role is missing");
        }
    }
}