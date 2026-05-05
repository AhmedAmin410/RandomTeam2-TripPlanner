package com.randmteam2.tripplanning.itinerary.security;

public class RoleAuthorizationHandler extends AuthHandler {

    private final String requiredRole;

    public RoleAuthorizationHandler(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void handle(AuthContext ctx) throws AuthException {
        if (requiredRole != null && !requiredRole.equals(ctx.getRole())) {
            throw new AuthException("Insufficient role: requires " + requiredRole, 403);
        }
        if (next != null) next.handle(ctx);
    }
}
