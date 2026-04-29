package com.randmteam2.tripplanning.user.security;

import com.randmteam2.tripplanning.user.security.AuthContext;

public class RoleAuthorizationHandler extends AuthHandler {
    private final String requiredRole;

    public RoleAuthorizationHandler(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (requiredRole != null) {
            String role = ctx.claims.get("role", String.class);
            if (!requiredRole.equals(role)) {
                throw new AuthException(403, "Insufficient role");
            }
        }
        proceed(ctx);
    }
}