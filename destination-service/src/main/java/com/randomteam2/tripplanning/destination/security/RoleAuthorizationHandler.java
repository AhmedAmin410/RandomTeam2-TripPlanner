package com.randomteam2.tripplanning.destination.security;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Set;

public class RoleAuthorizationHandler extends AuthHandler {

    private static final Set<String> ALLOWED_ROLES = Set.of("TRAVELER", "ADMIN");

    @Override
    protected void doHandle(AuthContext context) {
        if (!ALLOWED_ROLES.contains(context.getRole())) {
            throw new AuthenticationException(HttpServletResponse.SC_FORBIDDEN, "Insufficient role");
        }

        context.setAuthenticated(true);
    }
}