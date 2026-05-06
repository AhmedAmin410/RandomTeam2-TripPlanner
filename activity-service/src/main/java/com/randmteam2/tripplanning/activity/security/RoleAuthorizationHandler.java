package com.randmteam2.tripplanning.activity.security;

import com.randmteam2.tripplanning.activity.security.AuthContext;
import com.randmteam2.tripplanning.activity.security.AuthException;
import com.randmteam2.tripplanning.activity.security.AuthHandler;

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