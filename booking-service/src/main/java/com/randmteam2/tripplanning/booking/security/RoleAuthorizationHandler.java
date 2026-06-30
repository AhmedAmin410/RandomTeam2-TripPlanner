package com.randmteam2.tripplanning.booking.security;

public class RoleAuthorizationHandler extends AuthHandler {
    private final String requiredRole;

    public RoleAuthorizationHandler(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (requiredRole != null) {
            String role = normalizeRole(ctx.claims.get("role", String.class));
            if (!requiredRole.equals(role)) {
                throw new AuthException(403, "Insufficient role");
            }
        }
        proceed(ctx);
    }

    private static String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = role.trim().toUpperCase();
        if ("USER".equals(normalized) || "CUSTOMER".equals(normalized)) {
            return "TRAVELER";
        }
        return normalized;
    }
}
