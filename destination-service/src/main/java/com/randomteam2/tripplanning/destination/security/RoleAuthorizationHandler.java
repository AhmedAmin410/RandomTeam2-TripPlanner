package com.randomteam2.tripplanning.destination.security;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Set;

public class RoleAuthorizationHandler extends AuthHandler {

    private static final Set<String> ALLOWED_ROLES = Set.of("TRAVELER", "ADMIN", "USER", "CUSTOMER");

    @Override
    protected void doHandle(AuthContext context) {
        String role = context.getRole() != null ? context.getRole().trim().toUpperCase() : "";
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AuthenticationException(HttpServletResponse.SC_FORBIDDEN, "Insufficient role");
        }

        context.setRole(("USER".equals(role) || "CUSTOMER".equals(role)) ? "TRAVELER" : role);
        context.setAuthenticated(true);
    }
}
